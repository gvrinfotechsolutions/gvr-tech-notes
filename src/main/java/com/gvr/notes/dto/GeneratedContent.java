package com.gvr.notes.dto;

import lombok.Data;

@Data
public class GeneratedContent {

    private String markdownContent;
    private String codeSnippet;
    private String codeLanguage;
    private String tags;
    private String summary;
    private String error;
}
