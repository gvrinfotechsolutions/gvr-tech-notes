package com.gvr.notes.controller;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.gvr.notes.model.Subject;
import com.gvr.notes.model.Topic;
import com.gvr.notes.model.User;
import com.gvr.notes.pdf.PdfExportService;
import com.gvr.notes.service.MarkdownService;
import com.gvr.notes.service.SubjectService;
import com.gvr.notes.service.TopicProgressService;
import com.gvr.notes.service.TopicService;
import com.gvr.notes.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class TopicController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TopicService topicService;
    private final SubjectService subjectService;
    private final MarkdownService markdownService;
    private final TopicProgressService topicProgressService;
    private final UserService userService;
    private final PdfExportService pdfExportService;

    public TopicController(TopicService topicService,
                           SubjectService subjectService,
                           MarkdownService markdownService,
                           TopicProgressService topicProgressService,
                           UserService userService,
                           PdfExportService pdfExportService) {
        this.topicService = topicService;
        this.subjectService = subjectService;
        this.markdownService = markdownService;
        this.topicProgressService = topicProgressService;
        this.userService = userService;
        this.pdfExportService = pdfExportService;
    }

    @PostMapping("/saveTopic")
    public String saveTopic(@ModelAttribute Topic topic) {
        log.info("Saving topic: {}", topic.getTitle());
        topicService.saveTopic(topic);
        return "redirect:/";
    }

    @GetMapping("/export-pdf")
    public ResponseEntity<InputStreamResource> exportPdf() {
        Map<String, Object> response = topicService.getGroupedTopics(0, Integer.MAX_VALUE, "title", "asc");
        @SuppressWarnings("unchecked")
        Map<String, List<Topic>> groupedTopics = (Map<String, List<Topic>>) response.get("groupedTopics");
        ByteArrayInputStream pdf = pdfExportService.exportTopicsPdf(groupedTopics);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=GVR_Notes_Backup.pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    @GetMapping("/editTopic/{id}")
    public String editTopic(@PathVariable Long id, Model model) {
        model.addAttribute("topic", topicService.getTopicById(id));
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        return "edit-topic";
    }

    @GetMapping("/viewTopic/{id}")
    public String viewTopic(@PathVariable Long id, Model model) {
        log.info("View topic. topicId={}", id);
        Topic topic = topicService.getTopicById(id);
        String htmlContent = markdownService.convertMarkdownToHtml(topic.getContent());
        model.addAttribute("topic", topic);
        model.addAttribute("htmlContent", htmlContent);
        return "view-topic";
    }

    @PostMapping("/trackView/{id}")
    @ResponseBody
    public void trackView(@PathVariable Long id) {
        Topic topic = topicService.getTopicById(id);
        topic.setViewCount(topic.getViewCount() + 1);
        topic.setLastViewedAt(java.time.LocalDateTime.now());
        topicService.saveTopic(topic);
    }

    @GetMapping("/topics")
    public String topicsPage(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "title") String sortBy,
                              @RequestParam(defaultValue = "asc") String direction,
                              Model model, Principal principal) {
        size = Math.min(size, MAX_PAGE_SIZE);
        populateTopicsModel(page, size, sortBy, direction, model, principal);
        return "topics";
    }

    @GetMapping("/all-topics")
    public String viewAllTopics(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(defaultValue = "title") String sortBy,
                                 @RequestParam(defaultValue = "asc") String direction,
                                 Model model, Principal principal) {
        size = Math.min(size, MAX_PAGE_SIZE);
        populateTopicsModel(page, size, sortBy, direction, model, principal);
        return "topics";
    }

    @GetMapping("/subjectTopics/{id}")
    public String subjectTopics(@PathVariable Long id,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 Model model, Principal principal) {
        size = Math.min(size, MAX_PAGE_SIZE);

        Page<Topic> topicPage = topicService.getTopicsBySubject(id, page, size);
        Subject subject = subjectService.getSubjectById(id);

        Map<String, List<Topic>> groupedTopics = new LinkedHashMap<>();
        groupedTopics.put(subject.getName(), topicPage.getContent());

        model.addAttribute("subject", subject);
        model.addAttribute("groupedTopics", groupedTopics);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        model.addAttribute("topic", new Topic());
        model.addAttribute("selectedSubjectId", id);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", topicPage.getTotalPages());
        model.addAttribute("totalItems", topicPage.getTotalElements());
        model.addAttribute("selectedSize", size);

        User user = userService.findByUsername(principal.getName());
        model.addAttribute("progressMap", buildProgressMap(List.of(subject), user));
        model.addAttribute("completedTopicsMap", buildCompletedMap(topicPage.getContent(), user));
        model.addAttribute("totalTopicsMap", Map.of(id, topicService.countBySubjectId(id)));
        model.addAttribute("completedTopicsCountMap",
            Map.of(id, topicProgressService.getCompletedTopicsCount(user.getId(), id)));

        return "topics";
    }

    @GetMapping("/addTopic")
    public String addTopicPage(@RequestParam(required = false) Long subjectId, Model model) {
        Topic topic = new Topic();
        if (subjectId != null) topic.setSubject(subjectService.getSubjectById(subjectId));
        model.addAttribute("topic", topic);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        return "add-topic";
    }

    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/subjects")
    public String subjectsPage(Model model) {
        List<Subject> subjects = subjectService.getAllSubjects();
        model.addAttribute("subjects", subjects);
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        return "subjects";
    }

    @GetMapping("/completeTopic/{id}")
    public String completeTopic(@PathVariable Long id,
                                 @RequestParam(required = false) Long subjectId,
                                 Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Topic topic = topicService.getTopicById(id);
        topicProgressService.markTopicComplete(user, topic);
        return subjectId != null ? "redirect:/subjectTopics/" + subjectId : "redirect:/topics";
    }

    @GetMapping("/resetProgress/{subjectId}")
    public String resetProgress(@PathVariable Long subjectId,
                                 @RequestParam(required = false) Long returnSubjectId,
                                 Principal principal) {
        User user = userService.findByUsername(principal.getName());
        topicProgressService.resetSubjectProgress(user.getId(), subjectId);
        return returnSubjectId != null ? "redirect:/subjectTopics/" + returnSubjectId : "redirect:/topics";
    }

    @GetMapping("/searchTopics")
    public String searchTopics(@RequestParam String keyword,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Topic> topicPage = topicService.searchTopics(keyword, pageable);
        model.addAttribute("topics", topicPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", topicPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        model.addAttribute("totalResults", topicPage.getTotalElements());
        return "search-results";
    }

    @GetMapping("/filterByTag")
    public String filterByTag(@RequestParam String tag, Model model) {
        model.addAttribute("topics", topicService.filterByTag(tag));
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        model.addAttribute("topic", new Topic());
        return "topics";
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private void populateTopicsModel(int page, int size, String sortBy, String direction,
                                      Model model, Principal principal) {
        Map<String, Object> response = topicService.getGroupedTopics(page, size, sortBy, direction);
        @SuppressWarnings("unchecked")
        Map<String, List<Topic>> groupedTopics = (Map<String, List<Topic>>) response.get("groupedTopics");

        model.addAttribute("groupedTopics", groupedTopics);
        model.addAttribute("currentPage", response.get("currentPage"));
        model.addAttribute("totalPages", response.get("totalPages"));
        model.addAttribute("totalItems", response.get("totalItems"));
        model.addAttribute("selectedSize", size);

        List<Subject> subjects = subjectService.getAllSubjects();
        model.addAttribute("subjects", subjects);
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        model.addAttribute("topic", new Topic());

        User user = userService.findByUsername(principal.getName());
        model.addAttribute("progressMap", buildProgressMap(subjects, user));
        model.addAttribute("completedTopicsMap", buildCompletedMap(
            groupedTopics.values().stream().flatMap(List::stream).toList(), user));

        Map<Long, Long> totalTopicsMap = new HashMap<>();
        Map<Long, Long> completedTopicsCountMap = new HashMap<>();
        for (Subject subject : subjects) {
            totalTopicsMap.put(subject.getId(), topicService.countBySubjectId(subject.getId()));
            completedTopicsCountMap.put(subject.getId(),
                topicProgressService.getCompletedTopicsCount(user.getId(), subject.getId()));
        }
        model.addAttribute("totalTopicsMap", totalTopicsMap);
        model.addAttribute("completedTopicsCountMap", completedTopicsCountMap);
    }

    private Map<Long, Double> buildProgressMap(List<Subject> subjects, User user) {
        Map<Long, Double> progressMap = new HashMap<>();
        for (Subject subject : subjects) {
            progressMap.put(subject.getId(),
                topicProgressService.calculateSubjectProgress(user.getId(), subject.getId()));
        }
        return progressMap;
    }

    private Map<Long, Boolean> buildCompletedMap(List<Topic> topics, User user) {
        Map<Long, Boolean> completedMap = new HashMap<>();
        for (Topic topic : topics) {
            completedMap.put(topic.getId(),
                topicProgressService.isTopicCompleted(user.getId(), topic.getId()));
        }
        return completedMap;
    }
}
