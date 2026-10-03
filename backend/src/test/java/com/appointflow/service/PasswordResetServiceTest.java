package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ForgotPasswordRequest;
import com.appointflow.dto.ResetPasswordRequest;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.PasswordResetToken;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 1 - A1: Şifre sıfırlama testleri.
 * - forgot-password timing-safe (bilinmeyen email'de de hata fırlatmaz)
 * - reset-password: geçerli token, expired token, used token
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock KullaniciRepository kullaniciRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MailService mailService;
    @Mock StringRedisTemplate redisTemplate;

    @InjectMocks PasswordResetService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ttlMinutes", 60);
        ReflectionTestUtils.setField(service, "resetUrlBase", "http://localhost:3000/reset-password?token=");
    }

    @Test
    void forgotPassword_unknownEmail_returnsSilently() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("unknown@test.com");
        when(kullaniciRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Should NOT throw — timing-safe behavior
        service.requestReset(req);

        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_knownEmail_createsTokenAndSendsMail() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("known@test.com");
        Kullanici user = Kullanici.builder().id(1L).email("known@test.com").ad("Ali").build();
        when(kullaniciRepository.findByEmail("known@test.com")).thenReturn(Optional.of(user));

        service.requestReset(req);

        verify(tokenRepository).deleteByKullaniciId(1L);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getKullaniciId()).isEqualTo(1L);
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(59));
        verify(mailService).send(anyString(), anyString(), anyString());
    }

    @Test
    void resetPassword_invalidToken_throws400() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("nope");
        req.setYeniSifre("YeniSifre123");
        when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Token gecersiz");
    }

    @Test
    void resetPassword_expiredToken_throws400() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("expired");
        req.setYeniSifre("YeniSifre123");
        PasswordResetToken expired = PasswordResetToken.builder()
                .kullaniciId(1L)
                .token("expired")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("suresi dolmus");
    }

    @Test
    void resetPassword_usedToken_throws400() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("used");
        req.setYeniSifre("YeniSifre123");
        PasswordResetToken used = PasswordResetToken.builder()
                .kullaniciId(1L)
                .token("used")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .usedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(tokenRepository.findByToken("used")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("kullanilmis");
    }

    @Test
    void resetPassword_validToken_changesPasswordAndMarksTokenUsed() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("good");
        req.setYeniSifre("YeniSifre123");
        PasswordResetToken token = PasswordResetToken.builder()
                .id(99L)
                .kullaniciId(1L)
                .token("good")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com").sifre("OLD").build();
        when(tokenRepository.findByToken("good")).thenReturn(Optional.of(token));
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("YeniSifre123")).thenReturn("ENCODED_NEW");

        service.resetPassword(req);

        verify(kullaniciRepository).save(user);
        assertThat(user.getSifre()).isEqualTo("ENCODED_NEW");
        assertThat(token.getUsedAt()).isNotNull();
        verify(tokenRepository).save(token);
        verify(redisTemplate).delete("refresh_token:u@test.com");
    }
}
