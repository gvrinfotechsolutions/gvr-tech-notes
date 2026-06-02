package com.gvr.notes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

import com.gvr.notes.dto.RegisterRequest;
import com.gvr.notes.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {

        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {

        log.info("Registration page requested");

        model.addAttribute(
                "registerRequest",
                new RegisterRequest()
        );

        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute RegisterRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        log.info("User registration requested. username={}", request.getUsername());

        userService.registerUser(request);

        log.info("User registration submitted successfully. username={}", request.getUsername());

        return "redirect:/login?registered=true";
    }

    @GetMapping("/login")
    public String loginPage() {

        log.info("Login page requested");

        return "login";
    }
}