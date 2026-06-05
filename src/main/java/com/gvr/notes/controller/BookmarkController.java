package com.gvr.notes.controller;

import com.gvr.notes.model.Topic;
import com.gvr.notes.model.User;
import com.gvr.notes.model.UserBookmark;
import com.gvr.notes.service.BookmarkService;
import com.gvr.notes.service.SubjectService;
import com.gvr.notes.service.TopicService;
import com.gvr.notes.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserService userService;
    private final TopicService topicService;
    private final SubjectService subjectService;

    /** My Bookmarks page */
    @GetMapping("/bookmarks")
    public String bookmarksPage(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<UserBookmark> bookmarks = bookmarkService.getBookmarks(user.getId());
        model.addAttribute("bookmarks", bookmarks);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        return "bookmarks";
    }

    /** AJAX toggle — returns {"bookmarked": true/false} */
    @PostMapping("/bookmarks/toggle/{topicId}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long topicId, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Topic topic = topicService.getTopicById(topicId);
        boolean nowBookmarked = bookmarkService.toggle(user, topic);
        return Map.of("bookmarked", nowBookmarked);
    }

    /** Check bookmark status for a single topic */
    @GetMapping("/bookmarks/check/{topicId}")
    @ResponseBody
    public Map<String, Boolean> check(@PathVariable Long topicId, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        boolean bookmarked = bookmarkService.isBookmarked(user.getId(), topicId);
        return Map.of("bookmarked", bookmarked);
    }
}
