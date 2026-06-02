package com.gvr.notes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gvr.notes.model.Topic;
import com.gvr.notes.model.TopicNote;

public interface TopicNoteRepository extends JpaRepository<TopicNote, Long> {

	Optional<TopicNote> findByTopic(Topic topic);
	
	List<TopicNote> findAllByOrderByUpdatedDateDesc();
	
	Optional<TopicNote> findByTopicId(Long topicId);

}