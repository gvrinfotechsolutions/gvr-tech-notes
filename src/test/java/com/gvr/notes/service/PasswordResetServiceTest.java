package com.gvr.notes.service;

import com.gvr.notes.model.PasswordResetToken;
import com.gvr.notes.model.User;
import com.gvr.notes.repository.PasswordResetTokenRepository;
import com.gvr.notes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock JavaMailSender mailSender;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks PasswordResetService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("venky");
        user.setEmail("venky@example.com");
    }

    @Test
    void initiateReset_unknownEmail_doesNothing() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.initiateReset("unknown@example.com", "http://localhost:8091");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void initiateReset_knownEmail_sendsEmailWithResetLink() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.initiateReset(user.getEmail(), "http://localhost:8091");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains(user.getEmail());
        assertThat(sent.getText()).contains("http://localhost:8091/reset-password?token=");
    }

    @Test
    void validateToken_expiredToken_returnsEmpty() {
        PasswordResetToken expiredToken = mock(PasswordResetToken.class);
        when(expiredToken.isExpired()).thenReturn(true);
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        Optional<String> result = service.validateToken("expired-token");

        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_unknownToken_returnsEmpty() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThat(service.validateToken("bad-token")).isEmpty();
    }

    @Test
    void resetPassword_validToken_encodesAndSavesPassword() {
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.isExpired()).thenReturn(false);
        when(token.getUser()).thenReturn(user);
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass@1")).thenReturn("hashed");

        boolean result = service.resetPassword("valid-token", "NewPass@1");

        assertThat(result).isTrue();
        assertThat(user.getPassword()).isEqualTo("hashed");
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPassword_invalidToken_returnsFalse() {
        when(tokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThat(service.resetPassword("bad", "NewPass@1")).isFalse();
        verify(userRepository, never()).save(any());
    }
}
