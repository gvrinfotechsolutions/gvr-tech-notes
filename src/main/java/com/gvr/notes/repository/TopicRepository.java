package com.gvr.notes.repository;

import com.gvr.notes.model.Topic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository
        extends JpaRepository<Topic, Long> {

    List<Topic> findByTitleContainingIgnoreCase(String keyword);

    List<Topic> findByTagsContainingIgnoreCase(String tag);

    Page<Topic> findBySubjectId(Long subjectId, Pageable pageable);

    
    Page<Topic> findAllByOrderBySubject_NameAscTitleAsc(Pageable pageable);
    
    long countBySubjectId(Long subjectId);
    
        
    Page<Topic> findByTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(
            String title,
            String tags,
            Pageable pageable);

    List<Topic> findTop5ByLastViewedAtNotNullOrderByLastViewedAtDesc();

    long count();
}