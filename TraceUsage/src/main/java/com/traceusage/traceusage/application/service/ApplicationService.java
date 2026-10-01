package com.traceusage.traceusage.application.service;

import com.traceusage.traceusage.application.dto.ApplicationResponse;
import com.traceusage.traceusage.application.dto.CreateApplicationRequest;
import com.traceusage.traceusage.application.dto.CreateApplicationResponse;
import com.traceusage.traceusage.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    CreateApplicationResponse create(Long ownerId, CreateApplicationRequest request);

    PageResponse<ApplicationResponse> list(Long ownerId, Pageable pageable);

    ApplicationResponse getById(Long ownerId, Long applicationId);
}
