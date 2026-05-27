package com.gvr.notes.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.gvr.notes.model.Topic;
import com.gvr.notes.repository.TopicRepository;

@Service
public class TopicService {

	private final TopicRepository topicRepository;

	public TopicService(TopicRepository topicRepository) {
		this.topicRepository = topicRepository;
	}

	// SAVE TOPIC

	public Topic saveTopic(Topic topic) {
		return topicRepository.save(topic);
	}

	public List<Topic> getAllTopics() {
		return topicRepository.findAll();
	}

	// DELETE TOPIC

	public void deleteTopic(Long id) {
		topicRepository.deleteById(id);
	}

	// GET TOPIC BY ID

	public Topic getTopicById(Long id) {
		return topicRepository.findById(id).orElse(null);
	}

	// SEARCH TOPICS
	public List<Topic> searchTopics(String keyword) {

		return topicRepository.findByTitleContainingIgnoreCase(keyword);
	}

	public List<Topic> filterByTag(String tag) {

		return topicRepository.findByTagsContainingIgnoreCase(tag);
	}

	public Page<Topic> getTopicsBySubject(Long id, int page, int size) {

		Pageable pageable = PageRequest.of(page, size);

		return topicRepository.findBySubjectId(id, pageable);
	}

	public Page<Topic> getAllTopics(Pageable pageable) {

		return topicRepository.findAll(pageable);
	}

	public Map<String, Object> getGroupedTopics(

			int page,

			int size,

			String sortBy,

			String direction) {

		Sort sort = direction.equalsIgnoreCase("asc")

				? Sort.by(sortBy).ascending()

				: Sort.by(sortBy).descending();

		Pageable pageable = PageRequest.of(page, size, sort);

		Page<Topic> topicPage = topicRepository.findAll(pageable);

		Map<String, List<Topic>> groupedTopics = new LinkedHashMap<>();

		for (Topic topic : topicPage.getContent()) {

			String subjectName = topic.getSubject().getName();

			groupedTopics.computeIfAbsent(subjectName, k -> new ArrayList<>())

					.add(topic);
		}

		Map<String, Object> response = new HashMap<>();

		response.put("groupedTopics", groupedTopics);

		response.put("currentPage", page);

		response.put("totalPages", topicPage.getTotalPages());

		response.put("totalItems", topicPage.getTotalElements());

		return response;
	}
}