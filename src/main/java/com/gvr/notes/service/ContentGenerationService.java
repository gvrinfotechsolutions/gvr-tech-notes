package com.gvr.notes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gvr.notes.dto.GenerateRequest;
import com.gvr.notes.dto.GeneratedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class ContentGenerationService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneratedContent generate(GenerateRequest request) {

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Gemini API key not configured");
            GeneratedContent error = new GeneratedContent();
            error.setError("Gemini API key is not configured. Please set GEMINI_API_KEY environment variable.");
            return error;
        }

        try {
            String prompt = buildPrompt(request);

            // Build Gemini API request body
            Map<String, Object> body = Map.of(
                "contents", new Object[]{
                    Map.of("parts", new Object[]{
                        Map.of("text", prompt)
                    })
                },
                "generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "temperature", 0.7,
                    "maxOutputTokens", 4096
                )
            );

            log.info("Calling Gemini API for topic: {}", request.getTopicTitle());

            String response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);

        } catch (Exception e) {
            log.error("Gemini API call failed for topic: {}", request.getTopicTitle(), e);
            GeneratedContent error = new GeneratedContent();
            error.setError("Content generation failed: " + e.getMessage());
            return error;
        }
    }

    // ── Parse Gemini response ──────────────────────────────────────────────────

    private GeneratedContent parseResponse(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        String jsonText = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text")
                .asText();

        return objectMapper.readValue(jsonText, GeneratedContent.class);
    }

    // ── Prompt Builder ─────────────────────────────────────────────────────────

    private String buildPrompt(GenerateRequest request) {
        String contentTypeInstruction = getContentTypeInstruction(request.getContentType());

        return """
                You are a senior Java/Full Stack engineer and technical interview coach.
                Generate structured, interview-ready content for an engineering learning platform.

                Topic: %s
                Subject/Category: %s
                Content Type: %s

                %s

                TARGET AUDIENCE: Experienced engineers (3-8 years) preparing for FAANG, product, and service company interviews.

                Return ONLY a valid JSON object with EXACTLY these fields (no extra fields, no markdown wrapper):
                {
                  "markdownContent": "...",
                  "codeSnippet": "...",
                  "codeLanguage": "...",
                  "tags": "...",
                  "summary": "..."
                }

                FIELD RULES:

                markdownContent:
                - Use proper markdown: ## headings, **bold**, bullet lists, tables, blockquotes
                - Structure:
                  ## Overview
                  (2-3 line crisp explanation)

                  ## Key Concepts
                  (bullet points with bold terms)

                  ## How It Works Internally
                  (internals, lifecycle, mechanism — what interviewers love)

                  ## Interview Questions & Answers
                  (minimum 5 Q&As as blockquotes — real questions from actual interviews)
                  > **Q: question here?**
                  > A: answer here

                  ## Common Pitfalls
                  (what developers get wrong — critical for interview differentiation)

                  ## Best Practices
                  (production-grade tips)

                  ## Quick Reference
                  (table or bullet summary for last-minute revision)

                codeSnippet:
                - Real-world, production-quality code (NOT hello-world)
                - Must demonstrate the concept clearly
                - Include comments explaining key lines
                - No markdown code fences — raw code only

                codeLanguage:
                - Lowercase: "java", "python", "javascript", "typescript", "sql", "bash", "yaml", "json", "kotlin", "go"
                - Detect from subject/topic context

                tags:
                - 4-6 comma-separated lowercase tags
                - e.g. "spring, beans, ioc, dependency-injection, java"

                summary:
                - One crisp sentence (max 15 words) describing the topic
                """.formatted(
                        request.getTopicTitle(),
                        request.getSubject(),
                        request.getContentType() != null ? request.getContentType() : "CONCEPT",
                        contentTypeInstruction
                );
    }

    private String getContentTypeInstruction(String contentType) {
        if (contentType == null) return "";
        return switch (contentType.toUpperCase()) {
            case "COMPARISON" -> """
                    COMPARISON MODE: Compare two or more technologies/concepts side-by-side.
                    Include a detailed comparison table. Cover when to use which and why.
                    Example: HashMap vs ConcurrentHashMap, @RestController vs @Controller
                    """;
            case "CODE_PATTERN" -> """
                    CODE PATTERN MODE: Focus on a design pattern, coding technique, or algorithm.
                    Provide a complete, runnable implementation. Include before/after refactoring example.
                    Cover time/space complexity if applicable.
                    """;
            case "SCENARIO" -> """
                    SCENARIO MODE: System design or scenario-based interview question.
                    Frame as "How would you design/implement X?" with step-by-step approach.
                    Include architecture decisions, trade-offs, and scalability considerations.
                    """;
            case "CHEAT_SHEET" -> """
                    CHEAT SHEET MODE: Quick reference format.
                    Heavy use of tables, bullet points, and code snippets.
                    Optimized for last-minute revision before an interview.
                    Cover all key facts, commands, annotations, or methods.
                    """;
            default -> """
                    CONCEPT MODE: Deep dive into the concept.
                    Cover theory, internals, real-world usage, and interview traps.
                    """;
        };
    }
}
