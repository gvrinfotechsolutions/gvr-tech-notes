package com.gvr.notes.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Entity
@Data
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    // NEW FIELD
    @Column(length = 10000)
    private String codeSnippet;
    
    @Column(length = 1000)
    private String tags;

    // MANY TOPICS BELONG TO ONE SUBJECT
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;
    
    @Column(nullable = false)
    private Long viewCount = 0L;

    private LocalDateTime lastViewedAt;
}