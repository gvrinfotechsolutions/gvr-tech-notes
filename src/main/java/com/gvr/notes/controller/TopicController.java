package com.gvr.notes.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import com.gvr.notes.pdf.PdfExportService;
import com.gvr.notes.service.MarkdownService;
import com.gvr.notes.service.SubjectService;
import com.gvr.notes.service.TopicService;
import java.io.ByteArrayInputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
public class TopicController {

	private final TopicService topicService;
	private final SubjectService subjectService;
	private final MarkdownService markdownService;

	@Autowired
	private PdfExportService pdfExportService;

	public TopicController(TopicService topicService, SubjectService subjectService, MarkdownService markdownService) {

		this.topicService = topicService;
		this.subjectService = subjectService;
		this.markdownService = markdownService;
	}

	// SAVE TOPIC
	@PostMapping("/saveTopic")
	public String saveTopic(@ModelAttribute Topic topic) {

		topicService.saveTopic(topic);

		return "redirect:/";
	}

	@GetMapping("/export-pdf")
	public ResponseEntity<InputStreamResource> exportPdf() {

		// GET GROUPED TOPICS

		Map<String, Object> response = topicService.getGroupedTopics(0, Integer.MAX_VALUE, "title", "asc");

		Map<String, List<Topic>> groupedTopics = (Map<String, List<Topic>>) response.get("groupedTopics");

		// GENERATE PDF

		ByteArrayInputStream pdf = pdfExportService.exportTopicsPdf(groupedTopics);

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=GVR_Notes_Backup.pdf");

		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
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

		model.addAttribute("topic", topicService.getTopicById(id));

		model.addAttribute("subjects", subjectService.getAllSubjects());

		return "edit-topic";
	}

	// TOPIC PAGE
	@GetMapping("/topics")
	public String topicsPage(

			@RequestParam(defaultValue = "0") int page,

			@RequestParam(defaultValue = "10") int size,

			@RequestParam(defaultValue = "title") String sortBy,

			@RequestParam(defaultValue = "asc") String direction,

			Model model) {

		Map<String, Object> response = topicService.getGroupedTopics(page, size, sortBy, direction);

		model.addAttribute("groupedTopics", response.get("groupedTopics"));

		model.addAttribute("currentPage", response.get("currentPage"));

		model.addAttribute("totalPages", response.get("totalPages"));

		model.addAttribute("totalItems", response.get("totalItems"));

		model.addAttribute("subjects", subjectService.getAllSubjects());

		model.addAttribute("topic", new Topic());

		return "topics";
	}

	// SEARCH TOPICS
	@GetMapping("/searchTopics")
	public String searchTopics(@RequestParam String keyword, Model model) {

		model.addAttribute("topics", topicService.searchTopics(keyword));

		model.addAttribute("subjects", subjectService.getAllSubjects());

		model.addAttribute("topic", new Topic());

		return "topics";
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

	@GetMapping("/viewTopic/{id}")
	public String viewTopic(@PathVariable Long id, Model model) {

		Topic topic = topicService.getTopicById(id);

		// UPDATE ANALYTICS

		/*
		 * topic.setViewCount(topic.getViewCount() + 1);
		 * 
		 * topic.setLastViewedAt(java.time.LocalDateTime.now());
		 * 
		 * topicService.saveTopic(topic);
		 */

		// MARKDOWN

		String htmlContent = markdownService.convertMarkdownToHtml(topic.getContent());

		model.addAttribute("topic", topic);

		model.addAttribute("htmlContent", htmlContent);

		return "view-topic";
	}

	@PostMapping("/trackView/{id}")
	@ResponseBody
	public void trackView(@PathVariable Long id) {

		Topic topic = topicService.getTopicById(id);

		topic.setViewCount(topic.getViewCount() + 1);

		topic.setLastViewedAt(java.time.LocalDateTime.now());

		topicService.saveTopic(topic);
	}

	// FILTER BY TAG
	@GetMapping("/filterByTag")
	public String filterByTag(@RequestParam String tag, Model model) {

		model.addAttribute("topics", topicService.filterByTag(tag));

		model.addAttribute("subjects", subjectService.getAllSubjects());

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

			Model model) {

		Page<Topic> topicPage = topicService.getTopicsBySubject(id, page, size);

		Subject subject = subjectService.getSubjectById(id);

		// GROUP SUBJECT TOPICS

		Map<String, List<Topic>> groupedTopics = new LinkedHashMap<>();

		groupedTopics.put(subject.getName(), topicPage.getContent());

		// SEND TO UI

		model.addAttribute("groupedTopics", groupedTopics);

		model.addAttribute("subjects", subjectService.getAllSubjects());

		model.addAttribute("topic", new Topic());

		model.addAttribute("selectedSubjectId", id);

		model.addAttribute("currentPage", page);

		model.addAttribute("totalPages", topicPage.getTotalPages());

		return "topics";
	}

	// ADD TOPIC PAGE

	@GetMapping("/addTopic")
	public String addTopicPage(@RequestParam(required = false) Long subjectId, Model model) {

		Topic topic = new Topic();

		if (subjectId != null) {

			topic.setSubject(subjectService.getSubjectById(subjectId));
		}

		model.addAttribute("topic", topic);

		model.addAttribute("subjects", subjectService.getAllSubjects());

		return "add-topic";
	}

	@GetMapping("/admin")
	public String adminDashboard() {

		return "admin-dashboard";
	}

	// SUBJECT PAGE

	@GetMapping("/subjects")
	public String subjectsPage(Model model) {

		model.addAttribute("subjects", subjectService.getAllSubjects());

		return "subjects";
	}

	@GetMapping("/all-topics")
	public String viewAllTopics(

			@RequestParam(defaultValue = "0") int page,

			@RequestParam(defaultValue = "10") int size,

			@RequestParam(defaultValue = "title") String sortBy,

			@RequestParam(defaultValue = "asc") String direction,

			Model model) {

		Map<String, Object> response = topicService.getGroupedTopics(page, size, sortBy, direction);

		model.addAttribute("groupedTopics", response.get("groupedTopics"));

		model.addAttribute("currentPage", response.get("currentPage"));

		model.addAttribute("totalPages", response.get("totalPages"));

		model.addAttribute("totalItems", response.get("totalItems"));

		return "topics";
	}

}