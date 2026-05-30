package com.gvr.notes.controller;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gvr.notes.model.Subject;
import com.gvr.notes.model.Topic;
import com.gvr.notes.model.User;
import com.gvr.notes.pdf.PdfExportService;
import com.gvr.notes.repository.TopicRepository;
import com.gvr.notes.service.MarkdownService;
import com.gvr.notes.service.SubjectService;
import com.gvr.notes.service.TopicProgressService;
import com.gvr.notes.service.TopicService;
import com.gvr.notes.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class TopicController {

	private final TopicService topicService;
	private final SubjectService subjectService;
	private final MarkdownService markdownService;
	private final TopicProgressService topicProgressService;
	private final TopicRepository topicRepository;

	private final UserService userService;

	@Autowired
	private PdfExportService pdfExportService;

	public TopicController(

			TopicService topicService,

			TopicRepository topicRepository, SubjectService subjectService,

			MarkdownService markdownService,

			TopicProgressService topicProgressService,

			UserService userService) {

		this.topicService = topicService;

		this.subjectService = subjectService;

		this.markdownService = markdownService;

		this.topicProgressService = topicProgressService;
		this.topicRepository = topicRepository;

		this.userService = userService;
	}

	// SAVE TOPIC
	@PostMapping("/saveTopic")
	public String saveTopic(@ModelAttribute Topic topic) {

	    log.info("Creating topic with title: {}", topic.getTitle());

	    topicService.saveTopic(topic);

	    log.info("Topic created successfully with title: {}", topic.getTitle());

	    return "redirect:/";
	}

	@GetMapping("/export-pdf")
	public ResponseEntity<InputStreamResource> exportPdf() {

	    log.info("PDF export requested");

	    Map<String, Object> response =
	            topicService.getGroupedTopics(0, Integer.MAX_VALUE, "title", "asc");

	    Map<String, List<Topic>> groupedTopics =
	            (Map<String, List<Topic>>) response.get("groupedTopics");

	    log.info("Generating PDF for {} subject groups", groupedTopics.size());

	    ByteArrayInputStream pdf =
	            pdfExportService.exportTopicsPdf(groupedTopics);

	    HttpHeaders headers = new HttpHeaders();

	    headers.add("Content-Disposition",
	            "attachment; filename=GVR_Notes_Backup.pdf");

	    log.info("PDF exported successfully");

	    return ResponseEntity.ok()
	            .headers(headers)
	            .contentType(MediaType.APPLICATION_PDF)
	            .body(new InputStreamResource(pdf));
	}

	// DELETE TOPIC
	/*
	 * @GetMapping("/deleteTopic/{id}") public String deleteTopic(@PathVariable Long
	 * id) {
	 * 
	 * topicService.deleteTopic(id);
	 * 
	 * return "redirect:/"; }
	 */

	// EDIT TOPIC
	@GetMapping("/editTopic/{id}")
	public String editTopic(@PathVariable Long id, Model model) {

	    log.info("Edit topic page requested for topicId={}", id);

	    model.addAttribute("topic", topicService.getTopicById(id));

	    model.addAttribute("subjects", subjectService.getAllSubjects());

	    model.addAttribute("subjectTopicCountMap",
	            subjectService.getSubjectTopicCountMap());

	    log.info("Topic data loaded successfully for topicId={}", id);

	    return "edit-topic";
	}

	// TOPIC PAGE
	@GetMapping("/topics")
	public String topicsPage(

			@RequestParam(defaultValue = "0") int page,

			@RequestParam(defaultValue = "10") int size,

			@RequestParam(defaultValue = "title") String sortBy,

			@RequestParam(defaultValue = "asc") String direction,

			Model model,

			Principal principal) {

		// GROUPED TOPICS

		Map<String, Object> response = topicService.getGroupedTopics(page, size, sortBy, direction);

		model.addAttribute("groupedTopics", response.get("groupedTopics"));

		model.addAttribute("currentPage", response.get("currentPage"));

		model.addAttribute("totalPages", response.get("totalPages"));

		model.addAttribute("totalItems", response.get("totalItems"));

		// SUBJECTS

		List<Subject> subjects = subjectService.getAllSubjects();
		model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());

		model.addAttribute("subjects", subjects);

		// LOGGED IN USER

		User user = userService.findByUsername(principal.getName());

		// PROGRESS MAP

		Map<Long, Double> progressMap = new HashMap<>();

		for (Subject subject : subjects) {

			double progress = topicProgressService.calculateSubjectProgress(user.getId(), subject.getId());

			progressMap.put(subject.getId(), progress);
		}

		// SEND TO UI

		model.addAttribute("progressMap", progressMap);

		// COMPLETED TOPICS MAP

		Map<Long, Boolean> completedTopicsMap = new HashMap<>();

		Map<String, List<Topic>> groupedTopics = (Map<String, List<Topic>>) response.get("groupedTopics");

		for (List<Topic> topicList : groupedTopics.values()) {

			for (Topic topic : topicList) {

				boolean completed = topicProgressService.isTopicCompleted(user.getId(), topic.getId());

				completedTopicsMap.put(topic.getId(), completed);
			}
		}

		// SEND TO UI

		model.addAttribute("completedTopicsMap", completedTopicsMap);
		Map<Long, Long> totalTopicsMap = new HashMap<>();

		Map<Long, Long> completedTopicsCountMap = new HashMap<>();

		for (Subject subject : subjects) {

			long totalTopics = topicRepository.countBySubjectId(subject.getId());

			long completedTopics = topicProgressService.getCompletedTopicsCount(user.getId(), subject.getId());

			totalTopicsMap.put(subject.getId(), totalTopics);

			completedTopicsCountMap.put(subject.getId(), completedTopics);
		}

		model.addAttribute("totalTopicsMap", totalTopicsMap);

		model.addAttribute("completedTopicsCountMap", completedTopicsCountMap);
		model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());

		model.addAttribute("topic", new Topic());

		return "topics";
	}

	// SEARCH TOPICS
	@GetMapping("/searchTopics")
	public String searchTopics(@RequestParam String keyword,
	                           @RequestParam(defaultValue = "0") int page,
	                           @RequestParam(defaultValue = "10") int size,
	                           Model model) {

	    log.info("Topic search requested. keyword='{}', page={}, size={}",
	            keyword, page, size);

	    Pageable pageable = PageRequest.of(page, size);

	    Page<Topic> topicPage = topicService.searchTopics(keyword, pageable);

	    log.info("Search completed. Total results={}, Total pages={}",
	            topicPage.getTotalElements(),
	            topicPage.getTotalPages());

	    log.debug("Topics found: {}",
	            topicPage.getContent()
	                     .stream()
	                     .map(Topic::getTitle)
	                     .toList());

	    model.addAttribute("topics", topicPage.getContent());

	    model.addAttribute("currentPage", page);

	    model.addAttribute("totalPages", topicPage.getTotalPages());

	    model.addAttribute("keyword", keyword);

	    model.addAttribute("size", size);

	    model.addAttribute("totalResults", topicPage.getTotalElements());

	    return "search-results";
	}

	// VIEW TOPIC DETAILS
	/*
	 * @GetMapping("/viewTopic/{id}") public String viewTopic(@PathVariable Long id,
	 * Model model) {
	 * 
	 * Topic topic = topicService.getTopicById(id);
	 * 
	 * String htmlContent =
	 * markdownService.convertMarkdownToHtml(topic.getContent());
	 * 
	 * model.addAttribute("topic", topic);
	 * 
	 * model.addAttribute("htmlContent", htmlContent);
	 * 
	 * return "view-topic"; }
	 */

	@GetMapping("/completeTopic/{id}")
	public String completeTopic(@PathVariable Long id,
	                            @RequestParam(required = false) Long subjectId,
	                            Principal principal) {

	    log.info("Topic completion requested. topicId={}", id);

	    User user = userService.findByUsername(principal.getName());

	    Topic topic = topicService.getTopicById(id);

	    topicProgressService.markTopicComplete(user, topic);

	    log.info("User {} completed topic {}",
	            user.getUsername(),
	            topic.getTitle());

	    if (subjectId != null) {

	        log.debug("Redirecting back to subject page. subjectId={}",
	                subjectId);

	        return "redirect:/subjectTopics/" + subjectId;
	    }

	    return "redirect:/topics";
	}

	@GetMapping("/resetProgress/{subjectId}")
	public String resetProgress(@PathVariable Long subjectId,
	                            @RequestParam(required = false) Long returnSubjectId,
	                            Principal principal) {

	    log.warn("Progress reset requested for subjectId={} by user={}",
	            subjectId,
	            principal.getName());

	    User user = userService.findByUsername(principal.getName());

	    topicProgressService.resetSubjectProgress(user.getId(), subjectId);

	    log.info("Progress reset completed for subjectId={}", subjectId);

	    if (returnSubjectId != null) {

	        log.debug("Redirecting to subject page. subjectId={}",
	                returnSubjectId);

	        return "redirect:/subjectTopics/" + returnSubjectId;
	    }

	    return "redirect:/topics";
	}

	@GetMapping("/viewTopic/{id}")
	public String viewTopic(@PathVariable Long id, Model model) {

	    log.info("View topic requested. topicId={}", id);

	    Topic topic = topicService.getTopicById(id);

	    log.debug("Topic loaded successfully. title={}", topic.getTitle());

	    // MARKDOWN

	    String htmlContent =
	            markdownService.convertMarkdownToHtml(topic.getContent());

	    log.debug("Markdown converted successfully for topicId={}", id);

	    model.addAttribute("topic", topic);

	    model.addAttribute("htmlContent", htmlContent);

	    log.info("Topic rendered successfully. topicId={}", id);

	    return "view-topic";
	}

	@PostMapping("/trackView/{id}")
	@ResponseBody
	public void trackView(@PathVariable Long id) {

	    log.debug("Tracking view for topicId={}", id);

	    Topic topic = topicService.getTopicById(id);

	    topic.setViewCount(topic.getViewCount() + 1);

	    topic.setLastViewedAt(java.time.LocalDateTime.now());

	    topicService.saveTopic(topic);

	    log.debug("View count updated for topicId={}, currentViewCount={}",
	            id,
	            topic.getViewCount());
	}

	// FILTER BY TAG
	@GetMapping("/filterByTag")
	public String filterByTag(@RequestParam String tag, Model model) {

	    log.info("Filtering topics by tag={}", tag);

	    List<Topic> topics = topicService.filterByTag(tag);

	    log.info("Found {} topics for tag={}",
	            topics.size(),
	            tag);

	    model.addAttribute("topics", topics);

	    model.addAttribute("subjects", subjectService.getAllSubjects());

	    model.addAttribute("subjectTopicCountMap",
	            subjectService.getSubjectTopicCountMap());

	    model.addAttribute("topic", new Topic());

	    return "topics";
	}

	/*
	 * // SUBJECT TOPICS
	 * 
	 * @GetMapping("/subjectTopics/{id}") public String subjectTopics(@PathVariable
	 * Long id, Model model) {
	 * 
	 * model.addAttribute("topics", topicService.getTopicsBySubject(id));
	 * 
	 * model.addAttribute("subjects", subjectService.getAllSubjects());
	 * 
	 * model.addAttribute("topic", new Topic());
	 * 
	 * model.addAttribute("selectedSubjectId", id); return "topics"; }
	 */

	@GetMapping("/subjectTopics/{id}")
	public String subjectTopics(

			@PathVariable Long id,

			@RequestParam(defaultValue = "0") int page,

			@RequestParam(defaultValue = "5") int size,

			Model model,

			Principal principal) {

		Page<Topic> topicPage = topicService.getTopicsBySubject(id, page, size);

		System.out.println("================================");

		System.out.println("Subject Id = " + id);

		System.out.println("Topics Count = " + topicPage.getContent().size());

		topicPage.getContent().forEach(t -> System.out.println("Topic = " + t.getTitle()));

		System.out.println("================================");

		Subject subject = subjectService.getSubjectById(id);

		model.addAttribute("subject", subject);

		// GROUP SUBJECT TOPICS

		Map<String, List<Topic>> groupedTopics = new LinkedHashMap<>();

		groupedTopics.put(subject.getName(), topicPage.getContent());

		// SEND TOPICS

		model.addAttribute("groupedTopics", groupedTopics);

		model.addAttribute("subjects", subjectService.getAllSubjects());

		model.addAttribute("topic", new Topic());

		model.addAttribute("selectedSubjectId", id);

		model.addAttribute("currentPage", page);

		model.addAttribute("totalPages", topicPage.getTotalPages());
		model.addAttribute("totalItems", topicPage.getTotalElements());

		model.addAttribute("selectedSize", size);

		// LOGGED IN USER

		User user = userService.findByUsername(principal.getName());

		// PROGRESS MAP

		Map<Long, Double> progressMap = new HashMap<>();

		double progress = topicProgressService.calculateSubjectProgress(user.getId(), id);

		progressMap.put(id, progress);

		model.addAttribute("progressMap", progressMap);

		// COMPLETED TOPICS MAP

		Map<Long, Boolean> completedTopicsMap = new HashMap<>();

		for (Topic topic : topicPage.getContent()) {

			boolean completed = topicProgressService.isTopicCompleted(user.getId(), topic.getId());

			completedTopicsMap.put(topic.getId(), completed);
		}

		model.addAttribute("completedTopicsMap", completedTopicsMap);

		// TOPIC COUNTS

		Map<Long, Long> totalTopicsMap = new HashMap<>();

		Map<Long, Long> completedTopicsCountMap = new HashMap<>();

		long totalTopics = topicRepository.countBySubjectId(id);

		long completedTopics = topicProgressService.getCompletedTopicsCount(user.getId(), id);

		System.out.println("Total Topics DB = " + totalTopics);
		System.out.println("Completed Topics DB = " + completedTopics);
		System.out.println("Progress DB = " + progress);

		totalTopicsMap.put(id, totalTopics);

		completedTopicsCountMap.put(id, completedTopics);

		model.addAttribute("totalTopicsMap", totalTopicsMap);
		model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
		model.addAttribute("completedTopicsCountMap", completedTopicsCountMap);

		return "topics";
	}

	// ADD TOPIC PAGE

	@GetMapping("/addTopic")
	public String addTopicPage(@RequestParam(required = false) Long subjectId,
	                           Model model) {

	    log.info("Add topic page requested. subjectId={}", subjectId);

	    Topic topic = new Topic();

	    if (subjectId != null) {

	        topic.setSubject(subjectService.getSubjectById(subjectId));

	        log.debug("Pre-selected subjectId={} for new topic",
	                subjectId);
	    }

	    model.addAttribute("topic", topic);

	    model.addAttribute("subjects", subjectService.getAllSubjects());

	    model.addAttribute("subjectTopicCountMap",
	            subjectService.getSubjectTopicCountMap());

	    model.addAttribute("subjectTopicCountMap",
	            subjectService.getSubjectTopicCountMap());

	    return "add-topic";
	}

	@GetMapping("/admin")
	public String adminDashboard() {

		return "admin-dashboard";
	}

	// SUBJECT PAGE

	@GetMapping("/subjects")
	public String subjectsPage(Model model) {

		List<Subject> subjects = subjectService.getAllSubjects();

		model.addAttribute("subjects", subjects);

		Map<Long, Long> subjectTopicCountMap = new HashMap<>();

		for (Subject subject : subjects) {

			long count = topicRepository.countBySubjectId(subject.getId());

			subjectTopicCountMap.put(subject.getId(), count);
		}

		model.addAttribute("subjectTopicCountMap", subjectTopicCountMap);

		return "subjects";
	}

	@GetMapping("/all-topics")
	public String viewAllTopics(

			@RequestParam(defaultValue = "0") int page,

			@RequestParam(defaultValue = "10") int size,

			@RequestParam(defaultValue = "title") String sortBy,

			@RequestParam(defaultValue = "asc") String direction,

			Model model,

			Principal principal) {

		// FETCH GROUPED TOPICS

		Map<String, Object> response = topicService.getGroupedTopics(page, size, sortBy, direction);

		model.addAttribute("groupedTopics", response.get("groupedTopics"));

		model.addAttribute("currentPage", response.get("currentPage"));

		model.addAttribute("totalPages", response.get("totalPages"));

		model.addAttribute("totalItems", response.get("totalItems"));

		model.addAttribute("selectedSize", size);

		// SUBJECTS

		List<Subject> subjects = subjectService.getAllSubjects();

		Map<Long, Long> subjectTopicCountMap = new HashMap<>();

		for (Subject subject : subjects) {

			long count = topicRepository.countBySubjectId(subject.getId());

			subjectTopicCountMap.put(subject.getId(), count);
		}
		model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());

		model.addAttribute("subjectTopicCountMap", subjectTopicCountMap);

		model.addAttribute("subjects", subjects);

		// LOGGED IN USER

		User user = userService.findByUsername(principal.getName());

		// PROGRESS MAP

		Map<Long, Double> progressMap = new HashMap<>();

		// TOPIC COUNTS

		Map<Long, Long> totalTopicsMap = new HashMap<>();

		Map<Long, Long> completedTopicsCountMap = new HashMap<>();

		for (Subject subject : subjects) {

			double progress = topicProgressService.calculateSubjectProgress(user.getId(), subject.getId());

			progressMap.put(subject.getId(), progress);

			long totalTopics = topicRepository.countBySubjectId(subject.getId());

			long completedTopics = topicProgressService.getCompletedTopicsCount(user.getId(), subject.getId());

			totalTopicsMap.put(subject.getId(), totalTopics);

			completedTopicsCountMap.put(subject.getId(), completedTopics);
		}

		model.addAttribute("progressMap", progressMap);

		model.addAttribute("totalTopicsMap", totalTopicsMap);
		model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());

		model.addAttribute("completedTopicsCountMap", completedTopicsCountMap);

		// COMPLETED TOPICS MAP

		Map<Long, Boolean> completedTopicsMap = new HashMap<>();

		Map<String, List<Topic>> groupedTopics = (Map<String, List<Topic>>) response.get("groupedTopics");

		for (List<Topic> topicList : groupedTopics.values()) {

			for (Topic topic : topicList) {

				boolean completed = topicProgressService.isTopicCompleted(user.getId(), topic.getId());

				completedTopicsMap.put(topic.getId(), completed);
			}
		}

		model.addAttribute("completedTopicsMap", completedTopicsMap);
		model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());

		model.addAttribute("topic", new Topic());

		return "topics";
	}

}