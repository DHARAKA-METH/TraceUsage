package com.traceusage.traceusage.openapi.entity;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "openapi_endpoints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spec_id", nullable = false)
    private OpenApiSpec spec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_endpoint_id")
    private ApiEndpoint apiEndpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint_path", nullable = false, length = 500)
    private String endpointPath;

    @Column(name = "operation_id", length = 255)
    private String operationId;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "deprecated", nullable = false)
    private Boolean deprecated;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    public static OpenApiEndpoint create(Application application,
                                         OpenApiSpec spec,
                                         ApiEndpoint apiEndpoint,
                                         String httpMethod,
                                         String endpointPath,
                                         String operationId,
                                         String summary,
                                         boolean deprecated) {
        OpenApiEndpoint endpoint = new OpenApiEndpoint();
        endpoint.application = Objects.requireNonNull(application);
        endpoint.spec = Objects.requireNonNull(spec);
        endpoint.apiEndpoint = apiEndpoint;
        endpoint.httpMethod = Objects.requireNonNull(httpMethod);
        endpoint.endpointPath = Objects.requireNonNull(endpointPath);
        endpoint.operationId = operationId;
        endpoint.summary = summary;
        endpoint.deprecated = deprecated;
        endpoint.discoveredAt = Instant.now();
        return endpoint;
    }
}
