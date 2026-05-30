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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TopicService {

	private final TopicRepository topicRepository;

	private final SolrService solrService;

	public TopicService(TopicRepository topicRepository, SolrService solrService) {

		this.topicRepository = topicRepository;
		this.solrService = solrService;
	}

	// SAVE TOPIC

	public Topic saveTopic(Topic topic) {

		log.info("Saving topic. title={}", topic.getTitle());

		Topic savedTopic = topicRepository.save(topic);

		log.info("Topic saved successfully. id={}, title={}", savedTopic.getId(), savedTopic.getTitle());

		try {

			log.info("Indexing topic in Solr. topicId={}", savedTopic.getId());

			solrService.indexTopic(savedTopic);

			log.info("Solr indexing completed successfully. topicId={}", savedTopic.getId());

		} catch (Exception e) {

			log.error("Failed to index topic in Solr. topicId={}", savedTopic.getId(), e);
		}

		return savedTopic;
	}

	public List<Topic> getAllTopics() {

		log.info("Fetching all topics");

		List<Topic> topics = topicRepository.findAll();

		log.info("Fetched {} topics", topics.size());

		return topics;
	}

	// DELETE TOPIC

	public void deleteTopic(Long id) {
		topicRepository.deleteById(id);
	}

	// GET TOPIC BY ID

	public Topic getTopicById(Long id) {

		log.debug("Fetching topic. topicId={}", id);

		Topic topic = topicRepository.findById(id).orElse(null);

		if (topic == null) {

			log.warn("Topic not found. topicId={}", id);

		} else {

			log.debug("Topic found. title={}", topic.getTitle());
		}

		return topic;
	}

	// SEARCH TOPICS
	/*
	 * public List<Topic> searchTopics(String keyword) {
	 * 
	 * try {
	 * 
	 * List<Long> topicIds = solrService.searchTopicIds(keyword);
	 * 
	 * System.out.println("Topic IDs = " + topicIds);
	 * 
	 * List<Topic> topics = topicRepository.findAllById(topicIds);
	 * 
	 * System.out.println("Topics Size = " + topics.size());
	 * 
	 * return topics;
	 * 
	 * } catch (Exception e) {
	 * 
	 * e.printStackTrace();
	 * 
	 * return new ArrayList<>(); } }
	 */

	public Page<Topic> searchTopics(String keyword, Pageable pageable) {

		log.info("Searching topics. keyword={}", keyword);

		Page<Topic> topics = topicRepository.findByTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(keyword, keyword,
				pageable);

		log.info("Search completed. results={}", topics.getTotalElements());

		return topics;
	}

	public List<Topic> filterByTag(String tag) {

		log.info("Filtering topics by tag={}", tag);

		List<Topic> topics = topicRepository.findByTagsContainingIgnoreCase(tag);

		log.info("Found {} topics for tag={}", topics.size(), tag);

		return topics;
	}

	public Page<Topic> getTopicsBySubject(Long id, int page, int size) {

		log.info("Fetching topics by subject. subjectId={}, page={}, size={}", id, page, size);

		Pageable pageable = PageRequest.of(page, size);

		Page<Topic> topics = topicRepository.findBySubjectId(id, pageable);

		log.info("Fetched {} topics for subjectId={}", topics.getNumberOfElements(), id);

		return topics;
	}

	public Page<Topic> getAllTopics(Pageable pageable) {

	    log.info("Fetching paginated topics");

	    Page<Topic> topics =
	            topicRepository.findAll(pageable);

	    log.info("Fetched {} topics",
	            topics.getNumberOfElements());

	    return topics;
	}

	public Map<String, Object> getGroupedTopics(

			int page,

			int size,

			String sortBy,

			String direction) {
		
		log.info(
			    "Fetching grouped topics. page={}, size={}, sortBy={}, direction={}",
			    page,
			    size,
			    sortBy,
			    direction);

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

		log.info(
			    "Grouped topics loaded successfully. subjects={}, totalItems={}",
			    groupedTopics.size(),
			    topicPage.getTotalElements());
		return response;
	}

}