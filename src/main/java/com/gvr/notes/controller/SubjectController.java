package com.gvr.notes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.gvr.notes.model.Subject;
import com.gvr.notes.service.SubjectService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class SubjectController {

	private final SubjectService subjectService;

	public SubjectController(SubjectService subjectService) {
		this.subjectService = subjectService;
	}

	// HOME PAGE
	@GetMapping("/")
	public String home(Model model) {

		log.info("Home page requested");

	    model.addAttribute("subjects",
	            subjectService.getAllSubjects());

	    model.addAttribute(
	            "subjectTopicCountMap",
	            subjectService.getSubjectTopicCountMap());

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