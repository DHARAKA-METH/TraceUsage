package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.apikey.service.ApiKeyAuthenticationService;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.telemetry.dto.IdentifiedApplicationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TelemetryAuthenticationService {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;

    public TelemetryAuthenticationService(ApiKeyAuthenticationService apiKeyAuthenticationService) {
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
    }

    public IdentifiedApplicationResponse identifyApplication(String rawApiKey) {
        Application application = apiKeyAuthenticationService.authenticate(rawApiKey);
        return IdentifiedApplicationResponse.from(application);
    }
}
