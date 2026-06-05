package com.gvr.notes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.gvr.notes.dto.RegisterRequest;
import com.gvr.notes.exception.UserAlreadyExistsException;
import com.gvr.notes.security.RateLimiter;
import com.gvr.notes.service.PasswordResetService;
import com.gvr.notes.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final RateLimiter rateLimiter;

    public AuthController(UserService userService,
                          PasswordResetService passwordResetService,
                          RateLimiter rateLimiter) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
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
            HttpServletRequest httpRequest,
            Model model
    ) {
        if (!rateLimiter.isAllowed(httpRequest.getRemoteAddr())) {
            model.addAttribute("rateLimited", "Too many attempts. Please wait a minute and try again.");
            return "register";
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        log.info("User registration requested. username={}", request.getUsername());

        try {
            userService.registerUser(request);
        } catch (UserAlreadyExistsException e) {
            log.warn("Registration failed — username already taken. username={}", request.getUsername());
            model.addAttribute("usernameConflict", e.getMessage());
            return "register";
        }

        log.info("User registration submitted successfully. username={}", request.getUsername());

        return "redirect:/login?registered=true";
    }

    @GetMapping("/login")
    public String loginPage() {

        log.info("Login page requested");

        return "login";
    }

    // ── FORGOT PASSWORD ──────────────────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(
            @RequestParam String email,
            HttpServletRequest request,
            Model model
    ) {
        if (!rateLimiter.isAllowed(request.getRemoteAddr())) {
            model.addAttribute("submitted", true); // show the generic message — don't reveal rate limiting
            return "forgot-password";
        }
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        try {
            passwordResetService.initiateReset(email, baseUrl);
        } catch (Exception e) {
            log.error("Error sending password reset email", e);
        }
        // Always show the same message (don't leak whether email exists)
        model.addAttribute("submitted", true);
        return "forgot-password";
    }

    // ── RESET PASSWORD ───────────────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        boolean valid = passwordResetService.validateToken(token).isPresent();
        model.addAttribute("token", token);
        model.addAttribute("tokenValid", valid);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmNewPassword,
            Model model
    ) {
        // Basic server-side check: passwords must match and meet minimum length
        if (!newPassword.equals(confirmNewPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("tokenValid", true);
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }

        if (newPassword.length() < 8 ||
            !newPassword.matches(".*[A-Z].*") ||
            !newPassword.matches(".*[0-9].*") ||
            !newPassword.matches(".*[^A-Za-z0-9].*")) {
            model.addAttribute("token", token);
            model.addAttribute("tokenValid", true);
            model.addAttribute("error", "Password must be at least 8 characters and include an uppercase letter, digit, and special character.");
            return "reset-password";
        }

        boolean success = passwordResetService.resetPassword(token, newPassword);
        if (!success) {
            model.addAttribute("token", token);
            model.addAttribute("tokenValid", false);
            model.addAttribute("error", "Reset link has expired or is invalid. Please request a new one.");
            return "reset-password";
        }

        return "redirect:/login?reset=true";
    }
}