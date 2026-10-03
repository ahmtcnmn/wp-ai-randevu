package com.appointflow.service;

import com.appointflow.audit.service.AuditService;
import com.appointflow.common.ApiException;
import com.appointflow.dto.TwoFactorEnableResponse;
import com.appointflow.dto.TwoFactorSetupResponse;
import com.appointflow.entity.Kullanici;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.TwoFactorRecoveryCodeRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock KullaniciRepository kullaniciRepository;
    @Mock TwoFactorRecoveryCodeRepository recoveryCodeRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;

    @InjectMocks TwoFactorService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "issuer", "AppointFlow");
    }

    @Test
    void beginSetup_returnsQrAndSecret() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com").twoFactorEnabled(false).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(kullaniciRepository.save(any(Kullanici.class))).thenAnswer(inv -> inv.getArgument(0));

        TwoFactorSetupResponse setup = service.beginSetup("u@test.com");

        assertThat(setup.getSecret()).isNotBlank();
        assertThat(setup.getQrCodeDataUri()).startsWith("data:image/png;base64,");
        assertThat(setup.getOtpauthUrl()).startsWith("otpauth://totp/");
        assertThat(user.getTwoFactorPendingSecret()).isNotNull();
    }

    @Test
    void beginSetup_alreadyEnabled_throws400() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com").twoFactorEnabled(true).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.beginSetup("u@test.com"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("zaten aktif");
    }

    @Test
    void verifyAndEnable_correctCode_activatesAndReturnsRecoveryCodes() throws Exception {
        // Setup
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com")
                .twoFactorEnabled(false).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(kullaniciRepository.save(any(Kullanici.class))).thenAnswer(inv -> inv.getArgument(0));

        TwoFactorSetupResponse setup = service.beginSetup("u@test.com");
        String secret = setup.getSecret();

        // Generate live TOTP code for the secret
        CodeGenerator gen = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        long currentBucket = new SystemTimeProvider().getTime() / 30;
        String validCode = gen.generate(secret, currentBucket);

        TwoFactorEnableResponse response = service.verifyAndEnable("u@test.com", validCode);

        assertThat(response.getRecoveryCodes()).hasSize(10);
        assertThat(user.getTwoFactorEnabled()).isTrue();
        assertThat(user.getTwoFactorSecret()).isEqualTo(secret);
        assertThat(user.getTwoFactorPendingSecret()).isNull();
    }

    @Test
    void verifyAndEnable_wrongCode_throws() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com")
                .twoFactorEnabled(false)
                .twoFactorPendingSecret("ABCDEFGHIJKLMNOP").build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyAndEnable("u@test.com", "000000"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("hatali");
    }

    @Test
    void verifyAndEnable_noPendingSecret_throws() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com")
                .twoFactorEnabled(false).twoFactorPendingSecret(null).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyAndEnable("u@test.com", "123456"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("kurulumu baslatilmali");
    }

    @Test
    void disable_correctPassword_deactivates() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com")
                .twoFactorEnabled(true).twoFactorSecret("X")
                .sifre("HASHED").build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rightpw", "HASHED")).thenReturn(true);
        when(kullaniciRepository.save(any(Kullanici.class))).thenAnswer(inv -> inv.getArgument(0));

        service.disable("u@test.com", "rightpw");

        assertThat(user.getTwoFactorEnabled()).isFalse();
        assertThat(user.getTwoFactorSecret()).isNull();
    }

    @Test
    void disable_wrongPassword_throws() {
        Kullanici user = Kullanici.builder().id(1L).email("u@test.com")
                .twoFactorEnabled(true).sifre("HASHED").build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        assertThatThrownBy(() -> service.disable("u@test.com", "wrong"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Sifre yanlis");
    }
}
