package com.gvr.notes.controller;

import com.gvr.notes.dto.RegisterRequest;
import com.gvr.notes.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {

        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {

        model.addAttribute(
                "registerRequest",
                new RegisterRequest()
        );

        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute RegisterRequest request
    ) {

        userService.registerUser(request);

        return "redirect:/login";
    }
    
    @GetMapping("/login")
    public String loginPage() {

        return "login";
    }
}