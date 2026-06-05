package com.gvr.notes.repository;

import com.gvr.notes.model.UserBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserBookmarkRepository extends JpaRepository<UserBookmark, Long> {

    List<UserBookmark> findByUserIdOrderByBookmarkedAtDesc(Long userId);

    Optional<UserBookmark> findByUserIdAndTopicId(Long userId, Long topicId);

    boolean existsByUserIdAndTopicId(Long userId, Long topicId);

    @Transactional
    void deleteByUserIdAndTopicId(Long userId, Long topicId);

    long countByUserId(Long userId);
}
