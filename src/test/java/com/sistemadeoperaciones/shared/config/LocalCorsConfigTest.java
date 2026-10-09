package com.sistemadeoperaciones.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalCorsConfigTest {
    private static final String LOCAL_ORIGIN = "http://127.0.0.1:5174";

    @Test
    void httpCorsAllowsLocalOriginOnlyWhenConfigured() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        SecurityConfig security = new SecurityConfig(mock(JwtAuthFilter.class), environment);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");

        ReflectionTestUtils.setField(security, "localOrigin", LOCAL_ORIGIN);
        CorsConfiguration standard = security.corsConfigurationSource().getCorsConfiguration(request);
        assertThat(standard.getAllowedOrigins()).contains("http://localhost:5173");
        assertThat(standard.getAllowedOrigins()).doesNotContain(LOCAL_ORIGIN);

        environment.setActiveProfiles("local");
        CorsConfiguration local = security.corsConfigurationSource().getCorsConfiguration(request);
        assertThat(local.getAllowedOrigins()).contains(LOCAL_ORIGIN);
    }

    @Test
    void websocketAllowsLocalOriginOnlyWhenConfigured() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        WebSocketConfig websocket = new WebSocketConfig(mock(JwtChannelInterceptor.class), environment);

        ReflectionTestUtils.setField(websocket, "localOrigin", LOCAL_ORIGIN);
        websocket.registerStompEndpoints(registry);
        verify(registration).setAllowedOriginPatterns(
                "http://localhost:5173", "https://se-estatus.com", "https://www.se-estatus.com");

        environment.setActiveProfiles("local");
        websocket.registerStompEndpoints(registry);
        verify(registration).setAllowedOriginPatterns(
                "http://localhost:5173", "https://se-estatus.com", "https://www.se-estatus.com", LOCAL_ORIGIN);
    }
}
