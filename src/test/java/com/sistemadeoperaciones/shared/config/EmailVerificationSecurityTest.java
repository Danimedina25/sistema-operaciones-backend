package com.sistemadeoperaciones.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWRvdWJsZWQtdG8tYXZvaWQtc2hvcnQta2V5LXRlc3Q=",
        "jwt.expiration=3600000",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=1025",
        "app.crypto.secret-key=0123456789abcdef0123456789abcdef",
        "app.frontend.activation-url=http://127.0.0.1:5174/activar-cuenta",
        "app.frontend.email-verification-url=http://127.0.0.1:5174/verificar-correo",
        "app.frontend.password-reset-url=http://127.0.0.1:5174/restablecer-password"
})
@AutoConfigureMockMvc
class EmailVerificationSecurityTest {

    @Autowired private MockMvc mvc;

    @Test
    void verificationLinkWorksWithoutLoginEvenWithStaleBearerHeader() throws Exception {
        mvc.perform(post("/api/admin/users/complete-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-test-token\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/admin/users/complete-email-verification")
                        .header("Authorization", "Bearer invalid-old-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-test-token\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
