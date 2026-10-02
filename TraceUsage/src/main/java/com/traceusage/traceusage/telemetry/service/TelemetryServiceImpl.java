package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.apikey.service.ApiKeyAuthenticationService;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import com.traceusage.traceusage.telemetry.repository.UsageEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TelemetryServiceImpl implements TelemetryService {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final UsageEventRepository usageEventRepository;

    public TelemetryServiceImpl(ApiKeyAuthenticationService apiKeyAuthenticationService,
                                UsageEventRepository usageEventRepository) {
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
        this.usageEventRepository = usageEventRepository;
    }

    @Override
    @Transactional
    public void collect(String apiKey, UsageEventRequest request) {
        Application application = apiKeyAuthenticationService.authenticate(apiKey);

        if (usageEventRepository.existsByEventId(request.eventId())) {
            return;
        }

        UsageEvent usageEvent = UsageEvent.create(
                request.eventId(),
                application,
                request.method().trim().toUpperCase(Locale.ROOT),
                request.endpoint().trim(),
                request.statusCode(),
                request.occurredAt());

        try {
            usageEventRepository.save(usageEvent);
        } catch (DataIntegrityViolationException exception) {
            if (usageEventRepository.existsByEventId(request.eventId())) {
                return;
            }
            throw exception;
        }
    }
}
