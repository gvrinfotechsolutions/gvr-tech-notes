package com.gvr.notes.service;

import com.gvr.notes.model.Subject;
import com.gvr.notes.model.Topic;
import com.gvr.notes.model.TopicNote;
import com.gvr.notes.repository.TopicNoteRepository;
import com.gvr.notes.service.impl.TopicNoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicNoteServiceImplTest {

    @Mock TopicNoteRepository topicNoteRepository;
    @Mock TopicService topicService;

    @InjectMocks TopicNoteServiceImpl service;

    private Topic topic;

    @BeforeEach
    void setUp() {
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Java");

        topic = new Topic();
        topic.setId(10L);
        topic.setTitle("Streams");
        topic.setSubject(subject);
    }

    @Test
    void saveOrUpdateNote_newNote_createsAndSaves() {
        when(topicService.getTopicById(10L)).thenReturn(topic);
        when(topicNoteRepository.findByTopic(topic)).thenReturn(Optional.empty());
        when(topicNoteRepository.save(any())).thenAnswer(inv -> {
            TopicNote n = inv.getArgument(0);
            n.setId(99L);
            return n;
        });

        TopicNote result = service.saveOrUpdateNote(10L, "# Streams\nContent here");

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getNoteContent()).isEqualTo("# Streams\nContent here");
        assertThat(result.getCreatedDate()).isNotNull();
        verify(topicNoteRepository).save(any(TopicNote.class));
    }

    @Test
    void saveOrUpdateNote_existingNote_updatesContent() {
        TopicNote existing = new TopicNote();
        existing.setId(5L);
        existing.setTopic(topic);
        existing.setNoteContent("old content");
        existing.setCreatedDate(LocalDateTime.now().minusDays(1));

        when(topicService.getTopicById(10L)).thenReturn(topic);
        when(topicNoteRepository.findByTopic(topic)).thenReturn(Optional.of(existing));
        when(topicNoteRepository.save(existing)).thenReturn(existing);

        TopicNote result = service.saveOrUpdateNote(10L, "new content");

        assertThat(result.getNoteContent()).isEqualTo("new content");
        assertThat(result.getUpdatedDate()).isNotNull();
        verify(topicNoteRepository, times(1)).save(existing);
    }

    @Test
    void saveOrUpdateNote_emptyContent_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.saveOrUpdateNote(10L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void saveOrUpdateNote_quillEmptyContent_throwsIllegalArgument() {
        // Quill editor empty state
        assertThatThrownBy(() -> service.saveOrUpdateNote(10L, "<p><br></p>"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countNotes_delegatesToRepository() {
        when(topicNoteRepository.count()).thenReturn(42L);
        assertThat(service.countNotes()).isEqualTo(42L);
    }

    @Test
    void countDistinctSubjects_delegatesToRepository() {
        when(topicNoteRepository.countDistinctSubjects()).thenReturn(3L);
        assertThat(service.countDistinctSubjects()).isEqualTo(3L);
    }
}
