package com.gvr.notes.controller;

import com.gvr.notes.dto.GenerateRequest;
import com.gvr.notes.dto.GeneratedContent;
import com.gvr.notes.service.ContentGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ContentGenerationController {

    private final ContentGenerationService contentGenerationService;

    @PostMapping("/admin/generate-content")
    public GeneratedContent generateContent(@RequestBody GenerateRequest request) {
        log.info("Content generation requested. topic={}, subject={}, type={}",
                request.getTopicTitle(), request.getSubject(), request.getContentType());
        return contentGenerationService.generate(request);
    }
}
