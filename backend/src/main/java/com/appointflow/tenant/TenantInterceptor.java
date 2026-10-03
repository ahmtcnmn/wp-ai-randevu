package com.appointflow.tenant;

import com.appointflow.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // SUPER_ADMIN hiçbir tenant'a bağlı değil — TenantContext set edilmez
                String role = jwtUtil.extractRole(token);
                if ("ROLE_SUPER_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
                    return true;
                }
                Long tenantId = jwtUtil.extractTenantId(token);
                if (tenantId != null) {
                    TenantContext.set(tenantId);
                }
            } catch (Exception ignored) {
                // Geçersiz token — SecurityConfig zaten reddedecek
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
