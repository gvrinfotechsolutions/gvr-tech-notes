package com.gvr.notes.controller;

import com.gvr.notes.dto.GenerateRequest;
import com.gvr.notes.dto.GeneratedContent;
import com.gvr.notes.service.ContentGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ContentGenerationController {

    private final ContentGenerationService contentGenerationService;

    // In-memory job store — keyed by jobId. Entries are removed after client picks them up.
    private final ConcurrentHashMap<String, GeneratedContent> jobResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jobStatus = new ConcurrentHashMap<>(); // PENDING | DONE | ERROR

    /**
     * Kick off async generation. Returns immediately with a jobId the client polls.
     */
    @PostMapping("/admin/generate-content")
    public Map<String, String> startGeneration(@RequestBody GenerateRequest request) {
        String jobId = UUID.randomUUID().toString();
        jobStatus.put(jobId, "PENDING");

        log.info("Content generation started. jobId={}, topic={}, subject={}",
                jobId, request.getTopicTitle(), request.getSubject());

        runAsync(jobId, request);

        return Map.of("jobId", jobId, "status", "PENDING");
    }

    /**
     * Poll endpoint — returns {"status":"PENDING"} or the full GeneratedContent when done.
     */
    @GetMapping("/admin/generate-content/status/{jobId}")
    public Object pollStatus(@PathVariable String jobId) {
        String status = jobStatus.getOrDefault(jobId, "UNKNOWN");

        if ("PENDING".equals(status)) {
            return Map.of("status", "PENDING");
        }

        // DONE or ERROR — hand the result to the client and clean up
        GeneratedContent result = jobResults.remove(jobId);
        jobStatus.remove(jobId);

        if (result == null) {
            return Map.of("status", "ERROR", "error", "Job result not found");
        }
        return result;
    }

    @Async
    protected void runAsync(String jobId, GenerateRequest request) {
        try {
            GeneratedContent result = contentGenerationService.generate(request);
            jobResults.put(jobId, result);
            jobStatus.put(jobId, result.getError() != null ? "ERROR" : "DONE");
            log.info("Content generation completed. jobId={}", jobId);
        } catch (Exception e) {
            log.error("Content generation failed. jobId={}", jobId, e);
            GeneratedContent err = new GeneratedContent();
            err.setError("Generation failed: " + e.getMessage());
            jobResults.put(jobId, err);
            jobStatus.put(jobId, "ERROR");
        }
    }
}
