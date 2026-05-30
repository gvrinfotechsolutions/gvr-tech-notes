package com.gvr.notes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gvr.notes.enums.Status;
import com.gvr.notes.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    
    List<User> findByStatus(Status status);
    

}