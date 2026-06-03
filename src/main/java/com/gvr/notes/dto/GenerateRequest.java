package com.gvr.notes.dto;

import lombok.Data;

@Data
public class GenerateRequest {

    private String topicTitle;
    private String subject;
    private String contentType; // CONCEPT, COMPARISON, CODE_PATTERN, SCENARIO, CHEAT_SHEET
}
