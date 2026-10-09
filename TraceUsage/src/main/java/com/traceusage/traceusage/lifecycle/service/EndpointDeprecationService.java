package com.traceusage.traceusage.lifecycle.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.dto.DeprecateEndpointRequest;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.entity.EndpointDeprecation;
import com.traceusage.traceusage.lifecycle.exception.EndpointNotFoundException;
import com.traceusage.traceusage.lifecycle.exception.InvalidDeprecationException;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import com.traceusage.traceusage.lifecycle.repository.EndpointDeprecationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class EndpointDeprecationService {

    private final ApiEndpointRepository endpointRepository;
    private final EndpointDeprecationRepository deprecationRepository;

    public EndpointDeprecationService(ApiEndpointRepository endpointRepository,
                                      EndpointDeprecationRepository deprecationRepository) {
        this.endpointRepository = endpointRepository;
        this.deprecationRepository = deprecationRepository;
    }

    @Transactional
    public EndpointDeprecation deprecate(Application application,
                                         Long endpointId,
                                         DeprecateEndpointRequest request) {
        ApiEndpoint endpoint = endpointRepository
                .findByIdAndApplicationId(endpointId, application.getId())
                .orElseThrow(EndpointNotFoundException::new);

        validateReplacementEndpoint(request.replacementEndpoint(), endpoint.getEndpointPath());
        validateTargetRemovalDate(request.targetRemovalDate());

        EndpointDeprecation deprecation = deprecationRepository.findByEndpointId(endpointId).orElse(null);

        if (deprecation == null) {
            return deprecationRepository.save(EndpointDeprecation.create(
                    endpoint,
                    request.reason().trim(),
                    normalize(request.replacementEndpoint()),
                    request.targetRemovalDate()));
        }

        deprecation.update(
                request.reason().trim(),
                normalize(request.replacementEndpoint()),
                request.targetRemovalDate());

        return deprecationRepository.save(deprecation);
    }

    private void validateReplacementEndpoint(String replacementEndpoint, String endpointPath) {
        if (replacementEndpoint == null || replacementEndpoint.isBlank()) {
            return;
        }

        String trimmed = replacementEndpoint.trim();

        if (!trimmed.startsWith("/")) {
            throw new InvalidDeprecationException("Replacement endpoint must start with /");
        }

        if (trimmed.equals(endpointPath)) {
            throw new InvalidDeprecationException("Replacement endpoint must differ from the deprecated endpoint");
        }
    }

    private void validateTargetRemovalDate(LocalDate targetRemovalDate) {
        if (targetRemovalDate == null) {
            return;
        }

        if (targetRemovalDate.isBefore(LocalDate.now())) {
            throw new InvalidDeprecationException("Target removal date must not be in the past");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}