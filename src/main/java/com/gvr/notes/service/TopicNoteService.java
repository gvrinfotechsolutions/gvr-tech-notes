package com.gvr.notes.service;

import java.util.List;
import java.util.Optional;

import com.gvr.notes.model.Topic;
import com.gvr.notes.model.TopicNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TopicNoteService {

    Optional<TopicNote> findByTopic(Topic topic);

    TopicNote save(TopicNote topicNote);
    
    
    TopicNote saveOrUpdateNote(Long topicId, String noteContent);
    
    Page<TopicNote> getAllNotes(Pageable pageable);
    
    TopicNote getNoteByTopicId(Long topicId);

    long countNotes();
}