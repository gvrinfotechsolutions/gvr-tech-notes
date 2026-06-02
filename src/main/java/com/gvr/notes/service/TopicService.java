package com.gvr.notes.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "viewCount", "lastViewedAt");

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public Topic saveTopic(Topic topic) {
        log.info("Saving topic. title={}", topic.getTitle());
        Topic savedTopic = topicRepository.save(topic);
        log.info("Topic saved. id={}, title={}", savedTopic.getId(), savedTopic.getTitle());
        return savedTopic;
    }

    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    public void deleteTopic(Long id) {
        topicRepository.deleteById(id);
    }

    public Topic getTopicById(Long id) {
        return topicRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Topic not found with id: " + id));
    }

    public Page<Topic> searchTopics(String keyword, Pageable pageable) {
        return topicRepository.findByTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(keyword, keyword, pageable);
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

    public long countBySubjectId(Long subjectId) {
        return topicRepository.countBySubjectId(subjectId);
    }

    public Map<String, Object> getGroupedTopics(int page, int size, String sortBy, String direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            log.warn("Invalid sortBy field '{}', defaulting to 'title'", sortBy);
            sortBy = "title";
        }
        Sort sort = direction.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Topic> topicPage = topicRepository.findAll(pageable);

        Map<String, List<Topic>> groupedTopics = new LinkedHashMap<>();
        for (Topic topic : topicPage.getContent()) {
            String subjectName = topic.getSubject().getName();
            groupedTopics.computeIfAbsent(subjectName, k -> new ArrayList<>()).add(topic);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("groupedTopics", groupedTopics);
        response.put("currentPage", page);
        response.put("totalPages", topicPage.getTotalPages());
        response.put("totalItems", topicPage.getTotalElements());
        return response;
    }
}
