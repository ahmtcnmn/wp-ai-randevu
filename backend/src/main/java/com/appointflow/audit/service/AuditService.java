package com.appointflow.audit.service;

import com.appointflow.audit.entity.AuditLog;
import com.appointflow.audit.repository.AuditLogRepository;
import com.appointflow.entity.Kullanici;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Audit log producer. Servisler bunu cagirip "kim ne yapti" kaydeder.
 * Async yazilir — ana akisi yavaslatmaz.
 *
 * KRITIK: Context (tenant, user, IP, UA) ANA thread'de cekilir, async worker'a snapshot olarak gecer.
 * @Async metodu icinde RequestContextHolder, SecurityContextHolder, TenantContext bos olur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final KullaniciRepository kullaniciRepository;

    public void log(String action, String entityType, Long entityId, Map<String, Object> details) {
        // Snapshot'i ANA thread'de cek (request scoped bilgiler burada erisilebilir)
        AuditContext ctx = captureContext();
        writeAsync(action, entityType, entityId, details, ctx);
    }

    public void log(String action) {
        log(action, null, null, null);
    }

    public void log(String action, Map<String, Object> details) {
        log(action, null, null, details);
    }

    /**
     * Snapshot ile async DB yazimi. Hata olursa swallow (ana akisi bozmaz).
     */
    @Async
    protected void writeAsync(String action, String entityType, Long entityId,
                              Map<String, Object> details, AuditContext ctx) {
        try {
            AuditLog entry = AuditLog.builder()
                    .tenantId(ctx.tenantId)
                    .userId(ctx.userId)
                    .userEmail(ctx.email)
                    .userRol(ctx.rol)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details != null ? new HashMap<>(details) : null)
                    .ipAddress(ctx.ip)
                    .userAgent(ctx.userAgent)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Audit log yazilamadi: action={}, hata={}", action, e.getMessage());
        }
    }

    private AuditContext captureContext() {
        AuditContext ctx = new AuditContext();
        ctx.tenantId = TenantContext.getTenantId();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                ctx.email = auth.getName();
                Optional<Kullanici> userOpt = kullaniciRepository.findByEmail(ctx.email);
                if (userOpt.isPresent()) {
                    ctx.userId = userOpt.get().getId();
                    ctx.rol = userOpt.get().getRol() != null ? userOpt.get().getRol().name() : null;
                }
            }
        } catch (Exception ignored) {
            // SecurityContext bos olabilir, sorun degil
        }

        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                ctx.ip = resolveIp(req);
                ctx.userAgent = req.getHeader("User-Agent");
            }
        } catch (Exception ignored) {
            // Request scope yoksa (scheduled job, async caller vs.) sessizce gec
        }

        return ctx;
    }

    private String resolveIp(HttpServletRequest req) {
        String xfwd = req.getHeader("X-Forwarded-For");
        if (xfwd != null && !xfwd.isBlank()) {
            return xfwd.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /** Ana thread'den async worker'a tasinan snapshot. */
    private static class AuditContext {
        Long tenantId;
        Long userId;
        String email;
        String rol;
        String ip;
        String userAgent;
    }
}
