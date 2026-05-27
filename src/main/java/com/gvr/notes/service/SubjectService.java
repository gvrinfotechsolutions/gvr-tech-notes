package com.gvr.notes.service;

import com.gvr.notes.model.Subject;
import com.gvr.notes.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    // SAVE SUBJECT
    
    public Subject saveSubject(Subject subject) {
        return subjectRepository.save(subject);
    }

    // GET ALL SUBJECTS
    
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    // DELETE SUBJECT
    
    public void deleteSubject(Long id) {
        subjectRepository.deleteById(id);
    }
    
 // GET SUBJECT BY ID
    public Subject getSubjectById(Long id) {

        return subjectRepository.findById(id).orElse(null);
    }
}