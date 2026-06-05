package com.gvr.notes.service;

import com.gvr.notes.model.PasswordResetToken;
import com.gvr.notes.model.User;
import com.gvr.notes.repository.PasswordResetTokenRepository;
import com.gvr.notes.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Looks up the user by email. If found, deletes any existing token,
     * generates a new one, and sends a reset email.
     * Always returns without revealing whether the email exists (security best practice).
     */
    @Transactional
    public void initiateReset(String email, String appBaseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email (silently ignored): {}", email);
            return;
        }

        User user = userOpt.get();

        // Remove any existing token for this user
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        String token = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(token, user));

        String resetLink = appBaseUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("GVR_T_Notes — Reset your password");
        message.setText(
                "Hi " + user.getUsername() + ",\n\n" +
                "You requested a password reset for your GVR_T_Notes account.\n\n" +
                "Click the link below to reset your password (valid for 1 hour):\n" +
                resetLink + "\n\n" +
                "If you did not request this, you can safely ignore this email.\n\n" +
                "— GVR_T_Notes Team"
        );

        mailSender.send(message);
        log.info("Password reset email sent to {}", email);
    }

    /**
     * Validates a reset token and returns the associated username, or empty if invalid/expired.
     */
    public Optional<String> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired())
                .map(t -> t.getUser().getUsername());
    }

    /**
     * Resets the password if the token is valid and not expired.
     * Returns true on success, false if token is invalid/expired.
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            log.warn("Password reset attempted with invalid/expired token");
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
        log.info("Password reset successfully for user: {}", user.getUsername());
        return true;
    }
}
