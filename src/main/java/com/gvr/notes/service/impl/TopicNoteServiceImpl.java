package com.gvr.notes.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gvr.notes.model.Topic;
import com.gvr.notes.model.TopicNote;
import com.gvr.notes.repository.TopicNoteRepository;
import com.gvr.notes.service.TopicNoteService;
import com.gvr.notes.service.TopicService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
public class TopicNoteServiceImpl implements TopicNoteService {

	private static final Logger log = LoggerFactory.getLogger(TopicNoteServiceImpl.class);

	private final TopicNoteRepository topicNoteRepository;

	private final TopicService topicService;

	public TopicNoteServiceImpl(TopicNoteRepository topicNoteRepository, TopicService topicService) {
		this.topicNoteRepository = topicNoteRepository;
		this.topicService = topicService;
	}

	@Override
	public Optional<TopicNote> findByTopic(Topic topic) {

		log.info("Fetching note for topic id={}", topic.getId());

		return topicNoteRepository.findByTopic(topic);
	}

	@Override
	public TopicNote save(TopicNote topicNote) {

		log.info("Saving note for topic id={}", topicNote.getTopic().getId());

		String content = topicNote.getNoteContent();

		if (content == null || content.trim().isEmpty() || "<p><br></p>".equals(content.trim())) {

			throw new IllegalArgumentException("Note content cannot be empty");
		}

		TopicNote savedNote = topicNoteRepository.save(topicNote);

		log.info("Note saved successfully for topic id={}", topicNote.getTopic().getId());

		return savedNote;
	}

	@Override
	@Transactional
	public TopicNote saveOrUpdateNote(Long topicId, String noteContent) {

		log.info("Saving note for topicId={}", topicId);

		if (noteContent == null || noteContent.trim().isEmpty() || "<p><br></p>".equals(noteContent.trim())) {

			log.warn("Attempted to save empty note for topicId={}", topicId);

			throw new IllegalArgumentException("Note content cannot be empty");
		}

		Topic topic = topicService.getTopicById(topicId);

		Optional<TopicNote> existingNote = topicNoteRepository.findByTopic(topic);

		TopicNote topicNote;

		if (existingNote.isPresent()) {

			topicNote = existingNote.get();

			topicNote.setNoteContent(noteContent);

			topicNote.setUpdatedDate(LocalDateTime.now());

			log.info("Updating existing note for topicId={}", topicId);

		} else {

			topicNote = new TopicNote();

			topicNote.setTopic(topic);

			topicNote.setNoteContent(noteContent);

			topicNote.setCreatedDate(LocalDateTime.now());

			topicNote.setUpdatedDate(LocalDateTime.now());

			log.info("Creating new note for topicId={}", topicId);
		}

		TopicNote savedNote = topicNoteRepository.save(topicNote);

		log.info("Note saved successfully. noteId={}, topicId={}", savedNote.getId(), topicId);

		return savedNote;
	}
	
	@Override
	public Page<TopicNote> getAllNotes(Pageable pageable) {

	    log.info("Fetching notes page {}",
	            pageable.getPageNumber());

	    Page<TopicNote> notes =
	            topicNoteRepository.findAll(pageable);

	    log.info("Fetched {} notes",
	            notes.getNumberOfElements());

	    return notes;
	}
	
	@Override
	public TopicNote getNoteByTopicId(Long topicId) {

	    log.info("Fetching note for topicId={}", topicId);

	    return topicNoteRepository
	            .findByTopicId(topicId)
	            .orElse(null);
	}
	
	@Override
	public long countNotes() {

	    log.info("Fetching total notes count");

	    long count = topicNoteRepository.count();

	    log.info("Total notes count={}", count);

	    return count;
	}

	@Override
	public long countDistinctSubjects() {
	    return topicNoteRepository.countDistinctSubjects();
	}
}