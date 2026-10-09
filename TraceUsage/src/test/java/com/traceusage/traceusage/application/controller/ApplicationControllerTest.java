package com.traceusage.traceusage.application.controller;

import com.traceusage.traceusage.application.dto.ApplicationResponse;
import com.traceusage.traceusage.application.dto.CreateApplicationResponse;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.service.ApplicationService;
import com.traceusage.traceusage.auth.config.SecurityConfig;
import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.auth.security.JwtService;
import com.traceusage.traceusage.shared.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdC1rZXktd2l0aC1hdC1sZWFzdC10aGlydHktdHdvLWJ5dGVz",
        "app.jwt.access-token-expiration=15m"
})
class ApplicationControllerTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(
            7L, "Dharaka", "dharaka@example.com", "password-hash");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void create_withJwtUser_shouldReturnCredentials() throws Exception {
        when(applicationService.create(eq(7L), any())).thenReturn(new CreateApplicationResponse(
                1L,
                "Product Service",
                "development",
                "proj_test",
                "tru_sk_raw-key",
                "tru_pk_raw-key",
                Instant.parse("2026-10-01T10:00:00Z")));

        mockMvc.perform(post("/api/applications")
                        .with(user(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Product Service",
                                  "environment": "development"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value("proj_test"))
                .andExpect(jsonPath("$.data.apiKey").value("tru_sk_raw-key"))
                .andExpect(jsonPath("$.data.publicIngestKey").value("tru_pk_raw-key"));
    }

    @Test
    void create_withoutJwt_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Product Service\",\"environment\":\"development\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void list_shouldReturnOwnedApplicationsWithoutApiKeys() throws Exception {
        ApplicationResponse application = new ApplicationResponse(
                1L,
                "Product Service",
                "development",
                "proj_test",
                Instant.parse("2026-10-01T10:00:00Z"));
        when(applicationService.list(eq(7L), any()))
                .thenReturn(new PageResponse<>(List.of(application), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/applications").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].projectId").value("proj_test"))
                .andExpect(jsonPath("$.data.content[0].apiKey").doesNotExist());
    }

    @Test
    void getById_whenNotOwned_shouldReturnNotFound() throws Exception {
        when(applicationService.getById(7L, 42L))
                .thenThrow(new ApplicationNotFoundException());

        mockMvc.perform(get("/api/applications/42").with(user(USER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Application not found"));
    }
}
