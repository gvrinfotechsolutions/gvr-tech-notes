package com.gvr.notes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gvr.notes.dto.GenerateRequest;
import com.gvr.notes.dto.GeneratedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ContentGenerationService {

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneratedContent generate(GenerateRequest request) {

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("PASTE_YOUR_GROQ_KEY_HERE")) {
            log.error("Groq API key not configured");
            GeneratedContent error = new GeneratedContent();
            error.setError("Groq API key is not configured. Please set GROQ_API_KEY in application.properties or as environment variable.");
            return error;
        }

        try {
            String prompt = buildComprehensivePrompt(request);

            // Groq uses OpenAI-compatible format
            // NOTE: response_format:json_object removed — JSON mode forces models to compress
            // content aggressively to ensure valid JSON. Free-form + manual extraction is better.
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "model",    model,
                "messages", List.of(
                    Map.of(
                        "role",    "user",
                        "content", prompt
                    )
                ),
                "temperature", 0.7,
                "max_tokens",  8000
            ));

            log.info("Calling Groq API. model={}, topic={}", model, request.getTopicTitle());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            log.info("Groq API response status: {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("Groq API error: {}", response.body());
                GeneratedContent error = new GeneratedContent();
                error.setError(extractFriendlyError(response.statusCode(), response.body()));
                return error;
            }

            return parseGroqResponse(response.body());

        } catch (Exception e) {
            log.error("Groq API call failed for topic: {}", request.getTopicTitle(), e);
            GeneratedContent error = new GeneratedContent();
            error.setError("Content generation failed: " + e.getMessage());
            return error;
        }
    }

    // ── Friendly error extraction ──────────────────────────────────────────────

    private String extractFriendlyError(int statusCode, String body) {
        try {
            JsonNode root    = objectMapper.readTree(body);
            JsonNode errNode = root.path("error");
            String   code    = errNode.path("code").asText("");
            String   message = errNode.path("message").asText("");

            if ("rate_limit_exceeded".equals(code)) {
                // Pull the retry-after time from the message, e.g. "Please try again in 1h39m57.888s"
                String retryIn = "";
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("try again in ([^.]+?\\.?\\d*s)")
                        .matcher(message);
                if (m.find()) retryIn = " Try again in " + m.group(1) + ".";
                return "Daily token limit reached on Groq free tier (100k tokens/day)." + retryIn
                        + " You can upgrade at console.groq.com or wait for the limit to reset.";
            }

            if ("model_decommissioned".equals(code)) {
                return "The configured AI model has been retired by Groq. "
                        + "Update groq.model in application.properties. "
                        + "Check console.groq.com/docs/models for current models.";
            }

            if ("authentication_error".equals(code) || statusCode == 401) {
                return "Invalid Groq API key. Check groq.api.key in application.properties.";
            }

            // Fallback: return just the message, not the raw JSON
            return message.isBlank() ? "Groq API error (HTTP " + statusCode + ")" : message;

        } catch (Exception e) {
            return "Groq API error (HTTP " + statusCode + ")";
        }
    }

    // ── Parse Groq/OpenAI response ─────────────────────────────────────────────

    private GeneratedContent parseGroqResponse(String rawResponse) throws Exception {
        JsonNode root  = objectMapper.readTree(rawResponse);
        String content = root
                .path("choices").get(0)
                .path("message")
                .path("content")
                .asText();

        return parseDelimitedResponse(content);
    }

    /**
     * Parses the plain-text delimited response format:
     *   ===ARTICLE_START=== ... ===ARTICLE_END===
     *   ===CODE_START===    ... ===CODE_END===
     *   ===META_START===    ... ===META_END===
     *
     * This avoids asking the model to write a 2500-word article inside a JSON string value,
     * which caused aggressive content compression due to JSON escaping overhead.
     */
    private GeneratedContent parseDelimitedResponse(String text) {
        GeneratedContent result = new GeneratedContent();

        result.setMarkdownContent(extractBlock(text, "===ARTICLE_START===", "===ARTICLE_END==="));
        result.setCodeSnippet(extractBlock(text, "===CODE_START===", "===CODE_END==="));

        String meta = extractBlock(text, "===META_START===", "===META_END===");
        if (meta != null) {
            for (String line : meta.split("\\r?\\n")) {
                if (line.startsWith("LANGUAGE:")) result.setCodeLanguage(line.substring(9).trim().toLowerCase());
                else if (line.startsWith("TAGS:"))  result.setTags(line.substring(5).trim());
                else if (line.startsWith("SUMMARY:")) result.setSummary(line.substring(8).trim());
            }
        }

        if (result.getMarkdownContent() == null || result.getMarkdownContent().isBlank()) {
            log.warn("Delimited format not found — falling back to full response as markdown content");
            result.setMarkdownContent(text.trim());
        }

        return result;
    }

    private String extractBlock(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        int end   = text.indexOf(endMarker);
        if (start == -1 || end == -1 || end <= start) return null;
        return text.substring(start + startMarker.length(), end).trim();
    }

    // ── Comprehensive All-In-One Prompt ───────────────────────────────────────

    private String buildComprehensivePrompt(GenerateRequest request) {
        return """
                You are a Principal Engineer with 15+ years of Java/Full Stack experience
                and a senior interviewer at Amazon, Google, Flipkart, Swiggy, and Razorpay.

                Topic: %s
                Subject: %s

                ══════════════════════════════════════════════════════════
                WHAT TO WRITE — QUALITY STANDARD
                ══════════════════════════════════════════════════════════

                Write like THIS (expert level):
                  "String is immutable in Java because: (1) the backing char[] (Java 8) or
                  byte[] (Java 9+ Compact Strings) is marked private final, so no code outside
                  String can modify it. (2) The String class is final — no subclass can override
                  methods to expose the array. (3) No setter methods exist — every method that
                  appears to modify a String (concat, replace, toUpperCase) returns a NEW object.
                  This design enables three things: String Pool sharing (JVM safely reuses literals
                  in Heap without risk), thread safety without synchronization, and hashCode caching
                  (computed once, stored in a field — making String the ideal HashMap key)."

                NOT like this (surface level):
                  "String is immutable. It cannot be changed after creation."

                ══════════════════════════════════════════════════════════
                ARTICLE STRUCTURE — WRITE ALL SECTIONS IN ORDER
                ══════════════════════════════════════════════════════════

                Write the FULL ARTICLE in plain markdown. Go DEEP on every section.
                Every section must teach something new — no padding, no repetition.

                ## What is [Topic]?
                Expert-level definition (3-4 sentences). What problem does it solve?
                Where does it fit in Java/ecosystem? Which Java version introduced or changed it?

                ## Why It Exists — The Problem It Solves
                What existed BEFORE this feature/concept? Show the "before vs after" concretely.

                ## How It Works Internally
                MOST IMPORTANT SECTION. Write 500+ words here.
                - JVM/memory/thread behavior — numbered steps
                - Internal data structures, class hierarchies
                - Java version changes (Java 8 → 9 → 11 → 17 → 21)
                - Text-based memory diagrams: Stack → Heap → String Pool
                - Every sentence must add technical value

                ## Core Concepts (minimum 8 concepts)
                **ConceptName**: precise 3-sentence explanation with edge cases.
                Cover every important sub-concept. No repetition with other sections.

                ## Comparison with Alternatives
                Markdown table: Feature | This | Alternative1 | Alternative2
                After table: "Use X when... Use Y when..." in plain English.

                ## Real-World Usage & Scenarios (minimum 4 scenarios)
                For each: what the problem is → how this solves it → what to watch out for.
                Include scale considerations (millions of records, high concurrency, microservices).

                ## Step-by-Step Code Walkthrough
                Walk through the code snippet line by line in plain English.
                Point out non-obvious parts. Show what breaks with common mistakes.

                ## Interview Questions & Answers
                WRITE EXACTLY 12 QUESTIONS — 4 Basic, 4 Intermediate, 4 Advanced.

                > **Q [Basic]: question?**
                > **A:** 3-4 sentences. Direct and complete.

                > **Q [Intermediate]: question?**
                > **A:** Deep answer. Explain the WHY.
                > *What most engineers get wrong:* common wrong answer and why it fails.

                > **Q [Advanced]: tricky question from product company interviews?**
                > **A:** Expert answer with JVM internals, version specifics, edge cases (6+ sentences).
                > *Follow-up:* what the interviewer asks next, and the answer.
                > *Weak answer:* what junior engineers say that reveals shallow understanding.

                Advanced questions MUST cover: JVM/memory internals, thread safety, performance, "What happens if..." debugging.

                ## Common Mistakes & Anti-Patterns (5-6 mistakes)
                **Mistake Name**: What engineers do wrong.
                **Why it's wrong**: Technical reason — JVM behavior, memory impact, performance.
                **Correct approach**: Specific fix.

                ## Performance & Optimization
                Time/space complexity with numbers: "StringBuilder is O(n), + in loop is O(n²)."
                JVM optimizations, production tuning, measurable impact.

                ## Java Version Evolution
                **Java X**: what changed, why it matters.
                Cover: Java 8 → Java 9 → Java 11 → Java 17 → Java 21 changes.

                ## Quick Revision Cheat Sheet
                - 5-line elevator pitch for "Tell me about [Topic]"
                - Key facts table (3 columns)
                - Top 5 interview points
                - 3 gotcha questions with one-line answers

                ══════════════════════════════════════════════════════════
                OUTPUT FORMAT — USE EXACTLY THIS STRUCTURE
                ══════════════════════════════════════════════════════════

                Write your response in this EXACT format with these EXACT separator lines:

                ===ARTICLE_START===
                [write the complete markdown article here — all sections above]
                ===ARTICLE_END===

                ===CODE_START===
                [60-100 lines of production-quality code — no markdown fences]
                [Show the most important AND tricky aspects — not a hello-world]
                [Inline comments on every non-obvious line]
                [Show a common mistake side-by-side with the correct approach]
                ===CODE_END===

                ===META_START===
                LANGUAGE: java
                TAGS: tag1, tag2, tag3, tag4, tag5, tag6
                SUMMARY: one sentence max 15 words describing this topic
                ===META_END===

                IMPORTANT: Do not add any text outside these sections.
                """.formatted(
                        request.getTopicTitle(),
                        request.getSubject()
                );
    }

}
