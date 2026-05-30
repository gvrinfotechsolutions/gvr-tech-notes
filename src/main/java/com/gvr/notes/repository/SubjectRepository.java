package com.gvr.notes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gvr.notes.model.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
	
	@Query("""
		       SELECT s
		       FROM Subject s
		       LEFT JOIN Topic t ON t.subject.id = s.id
		       GROUP BY s
		       ORDER BY COUNT(t) DESC
		       """)
		List<Subject> findAllOrderByTopicCountDesc();
	
	
	@Query("""
		       SELECT s.id, COUNT(t)
		       FROM Subject s
		       LEFT JOIN Topic t ON t.subject.id = s.id
		       GROUP BY s.id
		       """)
		List<Object[]> getSubjectTopicCounts();
}