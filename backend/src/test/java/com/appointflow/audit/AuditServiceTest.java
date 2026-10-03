package com.appointflow.audit;

import com.appointflow.audit.entity.AuditLog;
import com.appointflow.audit.repository.AuditLogRepository;
import com.appointflow.audit.service.AuditService;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.Role;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock KullaniciRepository kullaniciRepository;
    @Mock ObjectProvider<HttpServletRequest> requestProvider;
    @Mock HttpServletRequest request;

    @InjectMocks AuditService auditService;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L);
        var auth = new UsernamePasswordAuthenticationToken("u@test.com", null,
                java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        Kullanici user = Kullanici.builder().id(7L).email("u@test.com").rol(Role.OWNER).build();
        lenient().when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        lenient().when(requestProvider.getIfAvailable()).thenReturn(request);
        lenient().when(request.getHeader("User-Agent")).thenReturn("test-agent");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void log_capturesTenantUserRolAndContext() {
        auditService.log("APPOINTMENT_CREATE", "Randevu", 42L, Map.of("musteriId", 10L));

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(cap.capture());

        AuditLog saved = cap.getValue();
        assertThat(saved.getTenantId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getUserEmail()).isEqualTo("u@test.com");
        assertThat(saved.getUserRol()).isEqualTo("OWNER");
        assertThat(saved.getAction()).isEqualTo("APPOINTMENT_CREATE");
        assertThat(saved.getEntityType()).isEqualTo("Randevu");
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getDetails()).containsEntry("musteriId", 10L);
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("test-agent");
    }

    @Test
    void log_repositoryThrows_swallowsException() {
        when(auditLogRepository.save(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("DB down"));

        // Should NOT throw — audit hatasi ana akisi bozmamali
        auditService.log("LOGIN");
    }

    @Test
    void log_xForwardedFor_takesFirstIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 192.168.1.1");

        auditService.log("LOGIN");

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getIpAddress()).isEqualTo("203.0.113.5");
    }
}
