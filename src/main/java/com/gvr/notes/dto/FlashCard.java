package com.gvr.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FlashCard {
    private int index;
    private String question;
    private String answer;
    private String level; // Basic | Intermediate | Advanced
}
