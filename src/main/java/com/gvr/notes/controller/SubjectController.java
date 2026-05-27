package com.gvr.notes.controller;

import com.gvr.notes.model.Subject;
import com.gvr.notes.service.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SubjectController {

	private final SubjectService subjectService;

	public SubjectController(SubjectService subjectService) {
		this.subjectService = subjectService;
	}

	// HOME PAGE
	@GetMapping("/")
	public String home(Model model) {

	    model.addAttribute("subjects",
	            subjectService.getAllSubjects());

	    if (!model.containsAttribute("subject")) {
	        model.addAttribute("subject", new Subject());
	    }

	    return "index";
	}

	// SAVE SUBJECT
	@PostMapping("/saveSubject")
	public String saveSubject(@ModelAttribute Subject subject) {

		subjectService.saveSubject(subject);

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

		model.addAttribute("subject", subjectService.getSubjectById(id));

		model.addAttribute("subjects", subjectService.getAllSubjects());

		return "edit-subject";
	}
}