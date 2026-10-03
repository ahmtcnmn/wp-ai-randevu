package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.entity.EmailVerificationToken;
import com.appointflow.entity.Kullanici;
import com.appointflow.repository.EmailVerificationTokenRepository;
import com.appointflow.repository.KullaniciRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock KullaniciRepository kullaniciRepository;
    @Mock EmailVerificationTokenRepository tokenRepository;
    @Mock MailService mailService;

    @InjectMocks EmailVerificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ttlHours", 48);
        ReflectionTestUtils.setField(service, "verifyUrlBase", "http://localhost:3000/verify-email?token=");
    }

    @Test
    void sendVerification_alreadyVerified_doesNothing() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com").ad("Ali")
                .emailDogrulandi(true).build();

        service.sendVerification(user);

        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendVerification_notVerified_createsToken() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com").ad("Ali")
                .emailDogrulandi(false).build();

        service.sendVerification(user);

        verify(tokenRepository).deleteByKullaniciId(1L);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(mailService).send(eq("u@test.com"), anyString(), anyString());
    }

    @Test
    void resendVerification_alreadyVerified_throws400() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com")
                .emailDogrulandi(true).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resendVerification("u@test.com"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("zaten dogrulanmis");
    }

    @Test
    void verify_invalidToken_throws() {
        when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("nope"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Token gecersiz");
    }

    @Test
    void verify_expiredToken_throws() {
        EmailVerificationToken expired = EmailVerificationToken.builder()
                .kullaniciId(1L).token("e")
                .expiresAt(LocalDateTime.now().minusHours(1)).build();
        when(tokenRepository.findByToken("e")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verify("e"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void verify_validToken_marksUserVerified() {
        EmailVerificationToken tok = EmailVerificationToken.builder()
                .id(5L).kullaniciId(1L).token("good")
                .expiresAt(LocalDateTime.now().plusHours(10)).build();
        Kullanici user = Kullanici.builder().id(1L).emailDogrulandi(false).build();
        when(tokenRepository.findByToken("good")).thenReturn(Optional.of(tok));
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(user));

        service.verify("good");

        assertThat(user.getEmailDogrulandi()).isTrue();
        assertThat(tok.getUsedAt()).isNotNull();
        verify(kullaniciRepository).save(user);
        verify(tokenRepository).save(tok);
    }

    private static <T> T eq(T value) { return org.mockito.ArgumentMatchers.eq(value); }
}
