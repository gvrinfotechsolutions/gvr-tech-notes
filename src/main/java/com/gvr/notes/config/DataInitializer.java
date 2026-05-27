package com.gvr.notes.config;

import com.gvr.notes.model.User;
import com.gvr.notes.enums.Role;
import com.gvr.notes.enums.Status;
import com.gvr.notes.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if(userRepository.findByUsername("venky").isEmpty()) {

            User admin = new User();

            admin.setUsername("venky");

            admin.setPassword(
                    passwordEncoder.encode("V@777hills")
            );

            admin.setRole(Role.ADMIN);

            admin.setStatus(Status.APPROVED);

            userRepository.save(admin);

            System.out.println("ADMIN USER CREATED");
        }
    }
}