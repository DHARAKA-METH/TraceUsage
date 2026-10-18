package com.demo.controller;

import com.traceusage.sdk.config.TraceUsageAutoConfiguration;
import com.traceusage.sdk.service.TelemetryPublisher;
import com.traceusage.sdk.telemetry.UsageEvent;
import com.demo.service.DemoStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(DemoStore.class)
@ImportAutoConfiguration(TraceUsageAutoConfiguration.class)
@TestPropertySource(properties = {
        "traceusage.enabled=true",
        "traceusage.api-key=test-api-key",
        "traceusage.server-url=http://localhost:8080"
})
class ProductControllerInstrumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelemetryPublisher publisher;

    @Test
    void createProduct_shouldPublishPostProductEndpointUsage() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content("{\"name\":\"Demo product\",\"price\":1000,\"stock\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Demo product"));

        verify(publisher).publish(argThat(event ->
                event.method().equals("POST")
                        && event.endpoint().equals("/api/products")
                        && event.statusCode().equals(201)));
    }

    @Test
    void getProduct_withDifferentIds_shouldPublishNormalizedEndpointPattern() throws Exception {
        mockMvc.perform(get("/api/products/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(250000))
                .andExpect(jsonPath("$.legacyCode").value("LAP-001"));

        mockMvc.perform(get("/api/products/102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(102));

        verify(publisher, times(2)).publish(argThat(this::isNormalizedGetProductEvent));
    }

    private boolean isNormalizedGetProductEvent(UsageEvent event) {
        return event.method().equals("GET")
                && event.endpoint().equals("/api/products/{id}")
                && event.statusCode().equals(200);
    }
}
