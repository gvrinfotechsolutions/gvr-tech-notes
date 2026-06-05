package com.gvr.notes.dto;

import lombok.Data;

@Data
public class GenerateRequest {

    private String topicTitle;
    private String subject;
    private String contentType;     // CONCEPT, COMPARISON, CODE_PATTERN, SCENARIO, CHEAT_SHEET
    private String existingContent; // For REVIEW_AND_IMPROVE mode — pass current markdown
    private String aiMode;          // "GENERATE" (new) | "REVIEW_AND_IMPROVE" (edit existing)
}
