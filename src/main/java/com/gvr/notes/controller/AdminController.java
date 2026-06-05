package com.gvr.notes.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gvr.notes.model.Subject;
import com.gvr.notes.model.Topic;
import com.gvr.notes.service.DocumentImportService;
import com.gvr.notes.service.SubjectService;
import com.gvr.notes.service.TopicService;
import com.gvr.notes.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final UserService userService;
    private final TopicService topicService;
    private final SubjectService subjectService;
    private final DocumentImportService documentImportService;

    public AdminController(UserService userService,
                           TopicService topicService,
                           SubjectService subjectService,
                           DocumentImportService documentImportService) {
        this.userService = userService;
        this.topicService = topicService;
        this.subjectService = subjectService;
        this.documentImportService = documentImportService;
    }

    // ── PENDING USERS ────────────────────────────────────────────────────────

    @GetMapping("/pending-users")
    public String pendingUsers(Model model) {
        log.info("Fetching pending users");
        model.addAttribute("users", userService.getPendingUsers());
        return "pending-users";
    }

    @PostMapping("/approve/{id}")
    public String approveUser(@PathVariable Long id) {
        log.info("Approving user. userId={}", id);
        userService.approveUser(id);
        return "redirect:/admin/pending-users";
    }

    @PostMapping("/reject/{id}")
    public String rejectUser(@PathVariable Long id) {
        log.warn("Rejecting user. userId={}", id);
        userService.rejectUser(id);
        return "redirect:/admin/pending-users";
    }

    // ── DELETE TOPIC (Admin only) ────────────────────────────────────────────

    @PostMapping("/deleteTopic/{id}")
    public String deleteTopic(@PathVariable Long id, RedirectAttributes ra) {
        log.warn("Admin deleting topic. topicId={}", id);
        try {
            topicService.deleteTopic(id);
            ra.addFlashAttribute("success", "Topic deleted successfully.");
            log.info("Topic deleted. topicId={}", id);
        } catch (Exception e) {
            log.error("Failed to delete topic. topicId={}", id, e);
            ra.addFlashAttribute("error", "Could not delete topic: " + e.getMessage());
        }
        return "redirect:/admin/manage-topics";
    }

    // ── DELETE SUBJECT (Admin only) ──────────────────────────────────────────

    @PostMapping("/deleteSubject/{id}")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes ra) {
        log.warn("Admin deleting subject. subjectId={}", id);
        try {
            long topicCount = topicService.countBySubjectId(id);
            if (topicCount > 0) {
                ra.addFlashAttribute("error",
                    "Cannot delete subject — it still has " + topicCount
                    + " topic(s). Delete or reassign all topics first.");
                return "redirect:/admin/manage-topics";
            }
            subjectService.deleteSubject(id);
            ra.addFlashAttribute("success", "Subject deleted successfully.");
            log.info("Subject deleted. subjectId={}", id);
        } catch (Exception e) {
            log.error("Failed to delete subject. subjectId={}", id, e);
            ra.addFlashAttribute("error", "Could not delete subject: " + e.getMessage());
        }
        return "redirect:/admin/manage-topics";
    }

    // ── MANAGE TOPICS / SUBJECTS PAGE ────────────────────────────────────────

    @GetMapping("/manage-topics")
    public String manageTopics(Model model) {
        log.info("Admin manage-topics page");
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectTopicCountMap", subjectService.getSubjectTopicCountMap());
        model.addAttribute("allTopics", topicService.getAllTopics());
        return "admin-manage-topics";
    }

    // ── IMPORT TOPICS FROM DOCUMENT ──────────────────────────────────────────

    @GetMapping("/import-topics")
    public String importTopicsPage(Model model) {
        log.info("Admin import-topics page");
        model.addAttribute("subjects", subjectService.getAllSubjects());
        return "admin-import-topics";
    }

    @PostMapping("/import-topics")
    public String importTopics(@RequestParam("file") MultipartFile file,
                               @RequestParam("subjectId") Long subjectId,
                               RedirectAttributes ra) {
        log.info("Admin import-topics upload. subjectId={}, filename={}",
                subjectId, file.getOriginalFilename());

        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/admin/import-topics";
        }

        Subject subject = subjectService.getSubjectById(subjectId);
        if (subject == null) {
            ra.addFlashAttribute("error", "Subject not found.");
            return "redirect:/admin/import-topics";
        }

        try {
            Map<String, String> parsedTopics = documentImportService.parseDocument(file);

            if (parsedTopics.isEmpty()) {
                ra.addFlashAttribute("error",
                    "No topics found. For .md files: use '# Topic Title' (single #) as topic boundary. " +
                    "For .docx files: use Heading 1 style for topic titles.");
                return "redirect:/admin/import-topics";
            }

            List<String> created = new ArrayList<>();
            for (Map.Entry<String, String> entry : parsedTopics.entrySet()) {
                Topic topic = new Topic();
                topic.setTitle(entry.getKey());
                topic.setContent(entry.getValue());
                topic.setSubject(subject);
                topicService.saveTopic(topic);
                created.add(entry.getKey());
                log.info("Imported topic '{}' into subject '{}'",
                        entry.getKey(), subject.getName());
            }

            ra.addFlashAttribute("success",
                created.size() + " topic(s) imported into '"
                + subject.getName() + "': " + String.join(", ", created));

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/import-topics";
        } catch (Exception e) {
            log.error("Failed to import topics from document", e);
            ra.addFlashAttribute("error", "Import failed: " + e.getMessage());
            return "redirect:/admin/import-topics";
        }

        return "redirect:/admin/import-topics";
    }
}
