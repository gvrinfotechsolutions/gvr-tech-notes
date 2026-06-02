package com.gvr.notes.dto;

import lombok.Data;

@Data
public class TopicNoteRequest {

    private Long topicId;

    private String noteContent;

}