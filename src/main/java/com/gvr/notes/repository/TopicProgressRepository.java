package com.gvr.notes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gvr.notes.model.TopicProgress;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.repository.Modifying;

public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {

	Optional<TopicProgress> findByUserIdAndTopicId(Long userId, Long topicId);

	long countByUserIdAndTopicSubjectIdAndCompletedTrue(Long userId, Long subjectId);

	@Transactional
	@Modifying
	void deleteByUserIdAndTopicSubjectId(Long userId, Long subjectId);

	long countByUserIdAndTopic_Subject_IdAndCompletedTrue(Long userId, Long subjectId);

}