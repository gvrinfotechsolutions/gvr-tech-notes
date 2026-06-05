package com.gvr.notes.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Parses uploaded Markdown (.md), PDF, or Word (.docx) files and extracts topics.
 *
 * Topic boundary convention (all formats):
 *   A line starting with exactly "# " (single hash + space) marks a new topic title.
 *   Lines starting with "## " or deeper are treated as CONTENT headings within a topic
 *   and are preserved as-is in the markdown content — they are NOT topic separators.
 *
 * Example document structure:
 *   # Spring Boot Basics
 *   ## Introduction
 *   Spring Boot is a framework...
 *
 *   # Dependency Injection
 *   ## Theory
 *   DI is a design pattern...
 *
 * Supported file types: .md, .docx, .pdf
 */
@Service
@Slf4j
public class DocumentImportService {

    /**
     * Parse the uploaded file and return an ordered map of topicTitle -> markdownContent.
     */
    public Map<String, String> parseDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        if (filename.endsWith(".md") || filename.endsWith(".txt")) {
            return parseMarkdown(file.getInputStream());
        } else if (filename.endsWith(".pdf")) {
            return parsePdf(file.getInputStream());
        } else if (filename.endsWith(".docx")) {
            return parseDocx(file.getInputStream());
        } else {
            throw new IllegalArgumentException(
                "Unsupported file type. Please upload a Markdown (.md), PDF, or Word (.docx) file.");
        }
    }

    // ── MARKDOWN (.md / .txt) PARSING ────────────────────────────────────────

    private Map<String, String> parseMarkdown(InputStream inputStream) throws IOException {
        log.info("Parsing Markdown document for topic import");
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return splitByH1Headings(text);
    }

    // ── PDF PARSING ──────────────────────────────────────────────────────────

    private Map<String, String> parsePdf(InputStream inputStream) throws IOException {
        log.info("Parsing PDF document for topic import");
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return splitByH1Headings(text);
        }
    }

    // ── WORD (.docx) PARSING ─────────────────────────────────────────────────

    private Map<String, String> parseDocx(InputStream inputStream) throws IOException {
        log.info("Parsing Word document for topic import");
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            List<String> lines = new ArrayList<>();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text == null || text.isBlank()) continue;
                String style = para.getStyle();
                // H1 Word headings → topic title separator (# prefix)
                if (style != null && (style.equalsIgnoreCase("Heading1")
                        || style.equalsIgnoreCase("Heading 1")
                        || style.equals("1"))) {
                    lines.add("# " + text.trim());
                // H2 Word headings → content sub-headings (## prefix, stay inside topic)
                } else if (style != null && (style.equalsIgnoreCase("Heading2")
                        || style.equalsIgnoreCase("Heading 2")
                        || style.equals("2"))) {
                    lines.add("## " + text.trim());
                // H3 Word headings → content sub-sub-headings
                } else if (style != null && (style.equalsIgnoreCase("Heading3")
                        || style.equalsIgnoreCase("Heading 3")
                        || style.equals("3"))) {
                    lines.add("### " + text.trim());
                } else {
                    lines.add(text);
                }
            }
            return splitByH1Headings(String.join("\n", lines));
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    /** Returns true if content has meaningful text (not just separators/blank lines). */
    private boolean isMeaningfulContent(String content) {
        String stripped = content.replaceAll("(?m)^[-=]{2,}\\s*$", "").trim();
        return stripped.length() > 30;
    }

    // ── SPLITTER: H1 only ────────────────────────────────────────────────────

    /**
     * Splits text into topics using ONLY single-hash "# " headings as boundaries.
     * Double-hash "## " and deeper headings are treated as content within a topic.
     * Preamble text before the first "# " heading is discarded.
     */
    private Map<String, String> splitByH1Headings(String text) {
        Map<String, String> topics = new LinkedHashMap<>();
        String currentTitle = null;
        StringBuilder currentContent = new StringBuilder();
        boolean insideCodeBlock = false;

        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();

            // Track fenced code blocks (``` or ~~~) — never split inside them
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                insideCodeBlock = !insideCodeBlock;
            }

            // Only exact "# " (one hash) starts a new topic — "## " stays as content
            // Ignore "# " lines that are inside a fenced code block (shell comments etc.)
            if (!insideCodeBlock && trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                // Save previous topic (only if it has meaningful content)
                if (currentTitle != null && currentContent.length() > 0) {
                    String prevContent = currentContent.toString().trim();
                    if (isMeaningfulContent(prevContent)) {
                        topics.put(currentTitle, prevContent);
                    }
                }
                // Start new topic — strip the leading "# "
                currentTitle = trimmed.substring(2).trim();
                currentContent = new StringBuilder();
            } else {
                if (currentTitle != null) {
                    currentContent.append(line).append("\n");
                }
            }
        }

        // Save last topic
        if (currentTitle != null && currentContent.length() > 0) {
            String trimmed = currentContent.toString().trim();
            if (isMeaningfulContent(trimmed)) {
                topics.put(currentTitle, trimmed);
            }
        }

        log.info("Document parsed: {} topic(s) found", topics.size());
        return topics;
    }
}
