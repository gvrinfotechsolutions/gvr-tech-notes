package com.gvr.notes.service;

import com.gvr.notes.model.Topic;
import com.gvr.notes.model.User;
import com.gvr.notes.model.UserBookmark;
import com.gvr.notes.repository.UserBookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {

    private final UserBookmarkRepository bookmarkRepository;

    /**
     * Toggle bookmark for a topic. Returns true if now bookmarked, false if removed.
     */
    public boolean toggle(User user, Topic topic) {
        if (bookmarkRepository.existsByUserIdAndTopicId(user.getId(), topic.getId())) {
            bookmarkRepository.deleteByUserIdAndTopicId(user.getId(), topic.getId());
            log.debug("Bookmark removed. userId={}, topicId={}", user.getId(), topic.getId());
            return false;
        } else {
            UserBookmark bm = new UserBookmark();
            bm.setUser(user);
            bm.setTopic(topic);
            bookmarkRepository.save(bm);
            log.debug("Bookmark added. userId={}, topicId={}", user.getId(), topic.getId());
            return true;
        }
    }

    public List<UserBookmark> getBookmarks(Long userId) {
        return bookmarkRepository.findByUserIdOrderByBookmarkedAtDesc(userId);
    }

    public boolean isBookmarked(Long userId, Long topicId) {
        return bookmarkRepository.existsByUserIdAndTopicId(userId, topicId);
    }

    public long countBookmarks(Long userId) {
        return bookmarkRepository.countByUserId(userId);
    }
}
