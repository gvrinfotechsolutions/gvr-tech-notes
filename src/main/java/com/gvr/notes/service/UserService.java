package com.gvr.notes.service;

import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gvr.notes.dto.RegisterRequest;
import com.gvr.notes.enums.Role;
import com.gvr.notes.enums.Status;
import com.gvr.notes.model.User;
import com.gvr.notes.repository.UserRepository;
import com.gvr.notes.exception.UserAlreadyExistsException;
@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(RegisterRequest request) {

        if(userRepository.findByUsername(
                request.getUsername()
        ).isPresent()) {

        	throw new UserAlreadyExistsException(
        	        "Username already exists"
        	);
        }

        User user = new User();

        user.setUsername(request.getUsername());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(Role.USER);

        user.setStatus(Status.PENDING);

        userRepository.save(user);
    }
    
    
    public void approveUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setStatus(Status.APPROVED);

        userRepository.save(user);
    }
    
    
    public void rejectUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setStatus(Status.REJECTED);

        userRepository.save(user);
    }
    
    
    public List<User> getPendingUsers() {

        return userRepository.findByStatus(
                Status.PENDING
        );
    }
    
    
    
    public User findByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    		

}