package com.gvr.notes.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gvr.notes.model.Topic;
import com.gvr.notes.model.TopicProgress;
import com.gvr.notes.model.User;
import com.gvr.notes.repository.TopicProgressRepository;
import com.gvr.notes.repository.TopicRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TopicProgressService {

	private final TopicProgressRepository progressRepository;

	private final TopicRepository topicRepository;

	public TopicProgressService(TopicProgressRepository progressRepository, TopicRepository topicRepository) {

		this.progressRepository = progressRepository;
		this.topicRepository = topicRepository;
	}

	// MARK TOPIC COMPLETE

	public void markTopicComplete(User user, Topic topic) {

		log.info("Marking topic as completed. userId={}, topicId={}", user.getId(), topic.getId());

		Optional<TopicProgress> existing = progressRepository.findByUserIdAndTopicId(user.getId(), topic.getId());

		if (existing.isEmpty()) {

			TopicProgress progress = new TopicProgress();

			progress.setUser(user);

			progress.setTopic(topic);

			progress.setCompleted(true);

			progress.setCompletedAt(LocalDateTime.now());

			progressRepository.save(progress);

			log.info("Topic marked as completed successfully. userId={}, topicId={}", user.getId(), topic.getId());

		} else {

			log.debug("Topic already completed. userId={}, topicId={}", user.getId(), topic.getId());
		}
	}

	public boolean isTopicCompleted(Long userId, Long topicId) {

		return progressRepository.findByUserIdAndTopicId(userId, topicId).isPresent();
	}

	// RESET SUBJECT PROGRESS

	public void resetSubjectProgress(Long userId, Long subjectId) {

		log.warn("Resetting subject progress. userId={}, subjectId={}", userId, subjectId);

		progressRepository.deleteByUserIdAndTopicSubjectId(userId, subjectId);

		log.info("Subject progress reset successfully. userId={}, subjectId={}", userId, subjectId);
	}

	// CALCULATE SUBJECT %

	public double calculateSubjectProgress(Long userId, Long subjectId) {

		long totalTopics = topicRepository.countBySubjectId(subjectId);

		long completedTopics = progressRepository.countByUserIdAndTopicSubjectIdAndCompletedTrue(userId, subjectId);

		if (totalTopics == 0) {

			log.debug("No topics found for subjectId={}", subjectId);

			return 0;
		}

		double progress = ((double) completedTopics / totalTopics) * 100;

		double roundedProgress = Math.round(progress);

		log.debug("Progress calculated. userId={}, subjectId={}, progress={}%", userId, subjectId, roundedProgress);

		return roundedProgress;
	}

	public long getCompletedTopicsCount(Long userId, Long subjectId) {

		long completedCount = progressRepository.countByUserIdAndTopicSubjectIdAndCompletedTrue(userId, subjectId);

		log.debug("Completed topics count fetched. userId={}, subjectId={}, count={}", userId, subjectId,
				completedCount);

		return completedCount;
	}
	
	
	public long getCompletedTopicsCount(Long userId) {

	    long completedCount =
	            progressRepository.countByUserIdAndCompletedTrue(userId);

	    log.debug("Total completed topics fetched. userId={}, count={}",
	            userId,
	            completedCount);

	    return completedCount;
	}
}