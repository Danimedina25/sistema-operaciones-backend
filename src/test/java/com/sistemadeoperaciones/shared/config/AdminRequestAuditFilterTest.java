package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.shared.audit.AdminRequestAuditFilter;
import com.sistemadeoperaciones.shared.audit.service.AdminRequestAuditService;
import com.sistemadeoperaciones.shared.model.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminRequestAuditFilterTest {
    private final AdminRequestAuditService audit = mock(AdminRequestAuditService.class);
    private final AdminRequestAuditFilter filter = new AdminRequestAuditFilter(audit);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void as(String role) {
        var user = new AuthenticatedUser(15L, "admin@test.invalid", List.of(role));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void recordsAdminReadsAndResultsWithoutRequestData() throws Exception {
        as("ADMIN");
        when(audit.begin(15L, "GET", "/api/operations/7")).thenReturn(8L);
        var request = new MockHttpServletRequest("GET", "/api/operations/7");
        request.setQueryString("token=secret");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(403));
        verify(audit).begin(15L, "GET", "/api/operations/7");
        verify(audit).finish(8L, 403);
    }

    @Test void nonAdminRequestDoesNotCreateAnAdminAudit() throws Exception {
        as("DIRECCION");
        var called = new AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/operations"), new MockHttpServletResponse(),
                (req, res) -> called.set(true));
        assertThat(called).isTrue();
        verifyNoInteractions(audit);
    }

    @Test void auditFailureStopsAdminMutationBeforeDispatch() {
        as("ADMIN");
        when(audit.begin(15L, "POST", "/api/admin/users")).thenThrow(new IllegalStateException("audit unavailable"));
        var called = new AtomicBoolean();
        FilterChain chain = (req, res) -> called.set(true);
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest("POST", "/api/admin/users"),
                new MockHttpServletResponse(), chain)).isInstanceOf(IllegalStateException.class);
        assertThat(called).isFalse();
    }

    @Test void unexpectedFailureIsRecordedAsServerError() throws Exception {
        as("ADMIN");
        when(audit.begin(15L, "POST", "/api/admin/users")).thenReturn(9L);
        var request = new MockHttpServletRequest("POST", "/api/admin/users");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("failed");
        })).isInstanceOf(ServletException.class);
        verify(audit).finish(9L, 500);
    }
}
