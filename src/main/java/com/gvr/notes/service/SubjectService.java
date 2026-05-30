package com.gvr.notes.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gvr.notes.model.Subject;
import com.gvr.notes.repository.SubjectRepository;
import com.gvr.notes.repository.TopicRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubjectService {

    private final TopicRepository topicRepository;

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          TopicRepository topicRepository) {

        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
    }

    // SAVE SUBJECT

    public Subject saveSubject(Subject subject) {

        log.info("Saving subject. name={}", subject.getName());

        Subject savedSubject = subjectRepository.save(subject);

        log.info("Subject saved successfully. id={}, name={}",
                savedSubject.getId(),
                savedSubject.getName());

        return savedSubject;
    }

    // GET ALL SUBJECTS

    public List<Subject> getAllSubjects() {

        log.debug("Fetching all subjects");

        List<Subject> subjects =
                subjectRepository.findAllOrderByTopicCountDesc();

        log.debug("Fetched {} subjects", subjects.size());

        return subjects;
    }

    // DELETE SUBJECT

    public void deleteSubject(Long id) {

        log.warn("Deleting subject. subjectId={}", id);

        subjectRepository.deleteById(id);

        log.info("Subject deleted successfully. subjectId={}", id);
    }

    // GET SUBJECT BY ID

    public Subject getSubjectById(Long id) {

        log.debug("Fetching subject. subjectId={}", id);

        Subject subject =
                subjectRepository.findById(id).orElse(null);

        if (subject == null) {

            log.warn("Subject not found. subjectId={}", id);

        } else {

            log.debug("Subject found. name={}",
                    subject.getName());
        }

        return subject;
    }

    public Map<Long, Long> getSubjectTopicCountMap() {

        log.debug("Calculating subject topic counts");

        Map<Long, Long> countMap = new HashMap<>();

        List<Subject> subjects = getAllSubjects();

        for (Subject subject : subjects) {

            countMap.put(
                    subject.getId(),
                    topicRepository.countBySubjectId(subject.getId())
            );
        }

        log.debug("Generated topic count map for {} subjects",
                countMap.size());

        return countMap;
    }
}