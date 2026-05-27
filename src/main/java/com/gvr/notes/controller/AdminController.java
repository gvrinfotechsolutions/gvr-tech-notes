package com.gvr.notes.controller;

import com.gvr.notes.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

	private final UserService userService;

	public AdminController(UserService userService) {

		this.userService = userService;
	}

	@GetMapping("/pending-users")
	public String pendingUsers(Model model) {

		model.addAttribute("users", userService.getPendingUsers());

		return "pending-users";
	}

	@PostMapping("/approve/{id}")
	public String approveUser(@PathVariable Long id) {

		userService.approveUser(id);

		return "redirect:/admin/pending-users";
	}

	@PostMapping("/reject/{id}")
	public String rejectUser(@PathVariable Long id) {

		userService.rejectUser(id);

		return "redirect:/admin/pending-users";
	}
}