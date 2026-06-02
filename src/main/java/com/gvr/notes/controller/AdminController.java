package com.gvr.notes.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gvr.notes.model.User;
import com.gvr.notes.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

	private final UserService userService;



	public AdminController(UserService userService) {

		this.userService = userService;
		
	}

	@GetMapping("/pending-users")
	public String pendingUsers(Model model) {

	    log.info("Fetching pending users");

	    List<User> pendingUsers = userService.getPendingUsers();

	    log.info("Found {} pending users", pendingUsers.size());

	    model.addAttribute("users", pendingUsers);

	    return "pending-users";
	}

	@PostMapping("/approve/{id}")
	public String approveUser(@PathVariable Long id) {

	    log.info("User approval requested. userId={}", id);

	    userService.approveUser(id);

	    log.info("User approved successfully. userId={}", id);

	    return "redirect:/admin/pending-users";
	}

	@PostMapping("/reject/{id}")
	public String rejectUser(@PathVariable Long id) {

	    log.warn("User rejection requested. userId={}", id);

	    userService.rejectUser(id);

	    log.info("User rejected successfully. userId={}", id);

	    return "redirect:/admin/pending-users";
	}

	/*
	 * @GetMapping("/reindex-solr") public String reindexSolr(RedirectAttributes ra)
	 * {
	 * 
	 * log.info("Solr reindex operation started");
	 * 
	 * try {
	 * 
	 * solrService.reindexAllTopics();
	 * 
	 * log.info("Solr reindex completed successfully");
	 * 
	 * ra.addFlashAttribute("success", "Solr Reindex Completed Successfully");
	 * 
	 * } catch (Exception e) {
	 * 
	 * log.error("Solr reindex failed", e);
	 * 
	 * ra.addFlashAttribute("error", "Solr Reindex Failed"); }
	 * 
	 * return "redirect:/admin"; }
	 */
	
	
}