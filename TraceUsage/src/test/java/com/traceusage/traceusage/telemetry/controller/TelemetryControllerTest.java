package com.traceusage.traceusage.telemetry.controller;

import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.auth.config.SecurityConfig;
import com.traceusage.traceusage.auth.security.JwtService;
import com.traceusage.traceusage.telemetry.dto.IdentifiedApplicationResponse;
import com.traceusage.traceusage.telemetry.service.TelemetryAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryController.class)
@Import({SecurityConfig.class, TelemetryExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdC1rZXktd2l0aC1hdC1sZWFzdC10aGlydHktdHdvLWJ5dGVz",
        "app.jwt.access-token-expiration=15m"
})
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelemetryAuthenticationService telemetryAuthenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void identifySingleEvent_withValidApiKeyAndNoJwt_shouldReturnApplication() throws Exception {
        when(telemetryAuthenticationService.identifyApplication("tru_sk_valid"))
                .thenReturn(new IdentifiedApplicationResponse(
                        1L, "Product Service", "development", "proj_test"));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-TraceUsage-Key", "tru_sk_valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application identified successfully"))
                .andExpect(jsonPath("$.data.applicationId").value(1))
                .andExpect(jsonPath("$.data.projectId").value("proj_test"));
    }

    @Test
    void identifyBatchEvent_withValidApiKeyAndNoJwt_shouldReturnApplication() throws Exception {
        when(telemetryAuthenticationService.identifyApplication("tru_sk_valid"))
                .thenReturn(new IdentifiedApplicationResponse(
                        1L, "Product Service", "development", "proj_test"));

        mockMvc.perform(post("/api/v1/events/batch")
                        .header("X-TraceUsage-Key", "tru_sk_valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void identifySingleEvent_withMissingApiKey_shouldReturnUnauthorized() throws Exception {
        when(telemetryAuthenticationService.identifyApplication(null))
                .thenThrow(new InvalidApiKeyException("Invalid API key"));

        mockMvc.perform(post("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }

    @Test
    void identifySingleEvent_withInvalidOrRevokedApiKey_shouldReturnUnauthorized() throws Exception {
        when(telemetryAuthenticationService.identifyApplication("tru_sk_invalid"))
                .thenThrow(new InvalidApiKeyException("Invalid API key"));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-TraceUsage-Key", "tru_sk_invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }
}
