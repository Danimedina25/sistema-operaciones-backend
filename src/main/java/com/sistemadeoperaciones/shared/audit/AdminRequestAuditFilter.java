package com.sistemadeoperaciones.shared.audit;

import com.sistemadeoperaciones.shared.audit.service.AdminRequestAuditService;
import com.sistemadeoperaciones.shared.model.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Request-level trace for every authenticated Admin API call, including reads and denials. */
public class AdminRequestAuditFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AdminRequestAuditFilter.class);
    private final AdminRequestAuditService audit;

    public AdminRequestAuditFilter(AdminRequestAuditService audit) { this.audit = audit; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!request.getRequestURI().startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
                || !authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            chain.doFilter(request, response);
            return;
        }

        // Do not capture query strings, headers, credentials or request/response bodies.
        Long auditId = audit.begin(user.getUserId(), request.getMethod(), request.getRequestURI());
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException error) {
            failed = true;
            throw error;
        } finally {
            try {
                audit.finish(auditId, failed && response.getStatus() < 400 ? 500 : response.getStatus());
            } catch (RuntimeException failure) {
                // begin() is durable; a missing completion remains visible for investigation.
                log.error("Admin audit completion failed for id {}", auditId, failure);
            }
        }
    }
}
