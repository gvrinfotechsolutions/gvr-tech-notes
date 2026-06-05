package com.gvr.notes.model;

import com.gvr.notes.enums.Difficulty;
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

    @Column(length = 10000)
    private String codeSnippet;

    @Column(length = 50)
    private String codeLanguage;

    @Column(length = 1000)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty;

    // MANY TOPICS BELONG TO ONE SUBJECT
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private Long viewCount = 0L;

    private LocalDateTime lastViewedAt;
}