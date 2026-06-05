package com.gvr.notes.controller;

import com.gvr.notes.dto.FlashCard;
import com.gvr.notes.model.Topic;
import com.gvr.notes.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FlashcardController {

    private final TopicService topicService;

    @GetMapping("/flashcards/{topicId}")
    public String flashcards(@PathVariable Long topicId, Model model) {
        Topic topic = topicService.getTopicById(topicId);
        List<FlashCard> cards = parseFlashCards(topic.getContent());
        model.addAttribute("topic", topic);
        model.addAttribute("cards", cards);
        model.addAttribute("totalCards", cards.size());
        return "flashcards";
    }

    /**
     * Parses Q&A pairs from the markdown content.
     *
     * Matches patterns like:
     *   > **Q [Basic]: question text?**
     *   > **A:** answer text (possibly multiline)
     *
     * Also handles simpler patterns like:
     *   **Q: question**
     *   **A:** answer
     */
    private List<FlashCard> parseFlashCards(String content) {
        List<FlashCard> cards = new ArrayList<>();
        if (content == null || content.isBlank()) return cards;

        // Pattern 1: blockquote style  "> **Q [Level]: ...**\n> **A:** ..."
        Pattern blockquotePattern = Pattern.compile(
            ">\\s*\\*\\*Q\\s*\\[([^\\]]+)\\]:\\s*(.*?)\\*\\*\\s*\\n(?:>\\s*\\*\\*A:\\*\\*\\s*)(.*?)(?=\\n>\\s*\\*\\*Q|\\n##|\\n---|\\'|$)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        Matcher m = blockquotePattern.matcher(content);
        while (m.find()) {
            String level    = m.group(1).trim();
            String question = m.group(2).trim();
            String answer   = cleanAnswer(m.group(3));
            if (!question.isBlank() && !answer.isBlank()) {
                cards.add(new FlashCard(cards.size() + 1, question, answer, level));
            }
        }

        // Pattern 2: simple **Q:** / **A:** style (fallback)
        if (cards.isEmpty()) {
            Pattern simplePattern = Pattern.compile(
                "\\*\\*Q(?:\\s*\\[[^\\]]*\\])?:\\s*(.*?)\\*\\*\\s*\\n\\*\\*A:\\*\\*\\s*(.*?)(?=\\n\\*\\*Q|\\n##|\\'|$)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
            );
            Matcher m2 = simplePattern.matcher(content);
            while (m2.find()) {
                String question = m2.group(1).trim();
                String answer   = cleanAnswer(m2.group(2));
                if (!question.isBlank() && !answer.isBlank()) {
                    cards.add(new FlashCard(cards.size() + 1, question, answer, "General"));
                }
            }
        }

        log.debug("Parsed {} flashcards from topic content", cards.size());
        return cards;
    }

    /** Strip blockquote markers and inline markdown from answer text */
    private String cleanAnswer(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("(?m)^>\\s*", "")           // strip "> " blockquote prefix per line
            .replaceAll("\\*Follow-up.*", "")         // strip follow-up questions
            .replaceAll("\\*Weak answer.*", "")       // strip weak answer notes
            .replaceAll("\\*Common mistake.*", "")    // strip common mistake notes
            .replaceAll("\\*\\*([^*]+)\\*\\*", "$1") // bold to plain
            .replaceAll("`([^`]+)`", "$1")            // inline code to plain
            .replaceAll("\\n{3,}", "\n\n")            // collapse excessive newlines
            .trim();
    }
}
