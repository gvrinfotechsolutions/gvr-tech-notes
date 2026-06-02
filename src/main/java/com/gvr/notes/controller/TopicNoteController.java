package com.gvr.notes.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gvr.notes.dto.TopicNoteRequest;
import com.gvr.notes.model.Topic;
import com.gvr.notes.model.TopicNote;
import com.gvr.notes.service.TopicNoteService;
import com.gvr.notes.service.TopicService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TopicNoteController {

	private final TopicNoteService topicNoteService;

	private final TopicService topicService;

	@GetMapping("/notes/topic/{topicId}")
	@ResponseBody
	public String getNoteByTopic(@PathVariable Long topicId) {

		Topic topic = topicService.getTopicById(topicId);

		Optional<TopicNote> note = topicNoteService.findByTopic(topic);

		return note.map(TopicNote::getNoteContent).orElse("");
	}

	@PostMapping("/notes/save")
	@ResponseBody
	public String saveNote(@RequestBody TopicNoteRequest request) {

		log.info("Saving note for topicId={}", request.getTopicId());

		topicNoteService.saveOrUpdateNote(request.getTopicId(), request.getNoteContent());

		return "SUCCESS";
	}

	@GetMapping("/notes")
	public String viewAllNotes(

	        @RequestParam(defaultValue = "0") int page,

	        Model model) {

	    log.info("Loading Notes Revision Page. Page={}", page);

	    Page<TopicNote> notesPage =
	            topicNoteService.getAllNotes(PageRequest.of(page, 20));

	    model.addAttribute("notesPage", notesPage);

	    model.addAttribute("notes", notesPage.getContent());

	    long totalNotes = topicNoteService.countNotes();

	    model.addAttribute("totalNotes", totalNotes);

	    log.info("Loaded {} notes",
	            notesPage.getNumberOfElements());

	    return "notes-list";
	}

	@GetMapping("/notes/view/{topicId}")
	public String viewNotes(@PathVariable Long topicId, Model model) {

		TopicNote note = topicNoteService.getNoteByTopicId(topicId);

		if (note == null) {

			model.addAttribute("message", "No notes available yet.");

			return "notes-view";
		}

		model.addAttribute("note", note);

		return "notes-view";
	}

}