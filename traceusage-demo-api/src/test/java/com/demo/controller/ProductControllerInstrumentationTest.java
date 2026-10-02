package com.demo.controller;

import com.demo.traceusage.TelemetryPublisher;
import com.demo.traceusage.UsageEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(com.demo.traceusage.TraceUsageConfig.class)
class ProductControllerInstrumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelemetryPublisher publisher;

    @Test
    void createProduct_shouldPublishPostProductEndpointUsage() throws Exception {
        mockMvc.perform(post("/api/products"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Product created"));

        verify(publisher).publish(argThat(event ->
                event.method().equals("POST")
                        && event.endpoint().equals("/api/products")
                        && event.statusCode().equals(201)));
    }

    @Test
    void getProduct_withDifferentIds_shouldPublishNormalizedEndpointPattern() throws Exception {
        mockMvc.perform(get("/api/products/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Product 10"));

        mockMvc.perform(get("/api/products/20"))
                .andExpect(status().isOk())
                .andExpect(content().string("Product 20"));

        verify(publisher, times(2)).publish(argThat(this::isNormalizedGetProductEvent));
    }

    private boolean isNormalizedGetProductEvent(UsageEvent event) {
        return event.method().equals("GET")
                && event.endpoint().equals("/api/products/{id}")
                && event.statusCode().equals(200);
    }
}
