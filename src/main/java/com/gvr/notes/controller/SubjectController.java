package com.gvr.notes.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.gvr.notes.model.Subject;
import com.gvr.notes.service.SubjectService;
import com.gvr.notes.service.TopicProgressService;
import com.gvr.notes.service.TopicService;
import com.gvr.notes.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class SubjectController {

	private final SubjectService subjectService;
	private final TopicService topicService;
	private final TopicProgressService topicProgressService;
	private final UserService userService;

	public SubjectController(SubjectService subjectService,
	                         TopicService topicService,
	                         TopicProgressService topicProgressService,
	                         UserService userService) {
		this.subjectService = subjectService;
		this.topicService = topicService;
		this.topicProgressService = topicProgressService;
		this.userService = userService;
	}

	// HOME PAGE
	@GetMapping("/")
	public String home(Model model, Principal principal) {

		log.info("Home page requested");

	    model.addAttribute("subjects", subjectService.getAllSubjects());
	    model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());

	    // Real stats
	    model.addAttribute("totalTopics", topicService.countAll());
	    model.addAttribute("totalSubjects", subjectService.getAllSubjects().size());

	    // Per-user completed count
	    if (principal != null) {
	        var user = userService.findByUsername(principal.getName());
	        model.addAttribute("completedTopicsCount", topicProgressService.getCompletedTopicsCount(user.getId()));
	    } else {
	        model.addAttribute("completedTopicsCount", 0L);
	    }

	    // Continue Learning — last 5 viewed topics
	    model.addAttribute("recentTopics", topicService.getRecentlyViewedTopics());

	    if (!model.containsAttribute("subject")) {
	        model.addAttribute("subject", new Subject());
	    }

	    log.info("Home page loaded successfully");

	    return "index";
	}

	// SAVE SUBJECT
	@PostMapping("/saveSubject")
	public String saveSubject(@ModelAttribute Subject subject) {

		log.info("Saving subject. name={}", subject.getName());

		subjectService.saveSubject(subject);

		log.info("Subject saved successfully. name={}", subject.getName());

		return "redirect:/";
	}

	// DELETE SUBJECT
	/*
	 * @GetMapping("/deleteSubject/{id}") public String deleteSubject(@PathVariable
	 * Long id) {
	 *
	 * subjectService.deleteSubject(id);
	 *
	 * return "redirect:/"; }
	 */

	// EDIT SUBJECT
	@GetMapping("/editSubject/{id}")
	public String editSubject(@PathVariable Long id, Model model) {

		log.info("Edit subject page requested. subjectId={}", id);

		model.addAttribute("subject", subjectService.getSubjectById(id));

		model.addAttribute("subjects", subjectService.getAllSubjects());

		log.info("Subject data loaded successfully. subjectId={}", id);

		return "edit-subject";
	}
}