package com.traceusage.traceusage.telemetry.controller;

import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.auth.config.SecurityConfig;
import com.traceusage.traceusage.auth.security.JwtService;
import com.traceusage.traceusage.telemetry.dto.IdentifiedApplicationResponse;
import com.traceusage.traceusage.telemetry.service.TelemetryAuthenticationService;
import com.traceusage.traceusage.telemetry.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private TelemetryService telemetryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void collectSingleEvent_withValidApiKeyAndNoJwt_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("X-TraceUsage-Key", "tru_sk_valid")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "f83284af-6427-4f42-a713-29dd41449915",
                                  "method": "GET",
                                  "endpoint": "/api/products",
                                  "statusCode": 200,
                                  "occurredAt": "2026-10-02T04:45:00Z"
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event collected successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());
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
    void collectSingleEvent_withMissingApiKey_shouldReturnUnauthorized() throws Exception {
        doThrow(new InvalidApiKeyException("Invalid API key"))
                .when(telemetryService).collect(eq(null), any());

        mockMvc.perform(post("/api/v1/events")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "f83284af-6427-4f42-a713-29dd41449915",
                                  "method": "GET",
                                  "endpoint": "/api/products",
                                  "statusCode": 200,
                                  "occurredAt": "2026-10-02T04:45:00Z"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }

    @Test
    void collectSingleEvent_withInvalidOrRevokedApiKey_shouldReturnUnauthorized() throws Exception {
        doThrow(new InvalidApiKeyException("Invalid API key"))
                .when(telemetryService).collect(eq("tru_sk_invalid"), any());

        mockMvc.perform(post("/api/v1/events")
                        .header("X-TraceUsage-Key", "tru_sk_invalid")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventId": "f83284af-6427-4f42-a713-29dd41449915",
                                  "method": "GET",
                                  "endpoint": "/api/products",
                                  "statusCode": 200,
                                  "occurredAt": "2026-10-02T04:45:00Z"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid API key"));
    }

    @Test
    void collectSingleEvent_withInvalidRequestBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("X-TraceUsage-Key", "tru_sk_valid")
                        .contentType("application/json")
                        .content("""
                                {
                                  "method": "",
                                  "endpoint": "",
                                  "statusCode": 99
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }
}
