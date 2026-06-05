package com.gvr.notes.config;

import com.gvr.notes.model.User;
import com.gvr.notes.enums.Role;
import com.gvr.notes.enums.Status;
import com.gvr.notes.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:venky}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        var existing = userRepository.findByUsername(adminUsername);

        if (existing.isEmpty()) {
            // First run — create the admin user
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setStatus(Status.APPROVED);
            userRepository.save(admin);
            log.info("Admin user created: {}", adminUsername);

        } else {
            // User exists — ensure role and status are correct.
            // Guards against the case where admin was re-registered via
            // the registration form (which sets role=USER, status=PENDING).
            User admin = existing.get();
            boolean changed = false;

            if (admin.getRole() != Role.ADMIN) {
                log.warn("Admin user '{}' had role={} — correcting to ADMIN", adminUsername, admin.getRole());
                admin.setRole(Role.ADMIN);
                changed = true;
            }
            if (admin.getStatus() != Status.APPROVED) {
                log.warn("Admin user '{}' had status={} — correcting to APPROVED", adminUsername, admin.getStatus());
                admin.setStatus(Status.APPROVED);
                changed = true;
            }
            if (changed) {
                userRepository.save(admin);
                log.info("Admin user '{}' role/status corrected", adminUsername);
            }
        }
    }
}