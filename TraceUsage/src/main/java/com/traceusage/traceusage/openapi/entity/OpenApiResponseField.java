package com.traceusage.traceusage.openapi.entity;

import com.traceusage.traceusage.application.entity.Application;
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
@Table(name = "openapi_response_fields")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenApiResponseField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spec_id", nullable = false)
    private OpenApiSpec spec;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "openapi_endpoint_id", nullable = false)
    private OpenApiEndpoint openApiEndpoint;

    @Column(name = "response_status", nullable = false, length = 10)
    private String responseStatus;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "schema_name", length = 255)
    private String schemaName;

    @Column(name = "field_path", nullable = false, length = 1000)
    private String fieldPath;

    @Column(name = "field_type", length = 100)
    private String fieldType;

    @Column(name = "required", nullable = false)
    private Boolean required;

    @Column(name = "nullable", nullable = false)
    private Boolean nullable;

    @Column(name = "deprecated", nullable = false)
    private Boolean deprecated;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    public static OpenApiResponseField create(Application application,
                                              OpenApiSpec spec,
                                              OpenApiEndpoint openApiEndpoint,
                                              String responseStatus,
                                              String contentType,
                                              String schemaName,
                                              String fieldPath,
                                              String fieldType,
                                              boolean required,
                                              boolean nullable,
                                              boolean deprecated,
                                              String description) {
        OpenApiResponseField field = new OpenApiResponseField();
        field.application = Objects.requireNonNull(application);
        field.spec = Objects.requireNonNull(spec);
        field.openApiEndpoint = Objects.requireNonNull(openApiEndpoint);
        field.responseStatus = Objects.requireNonNull(responseStatus);
        field.contentType = Objects.requireNonNull(contentType);
        field.schemaName = schemaName;
        field.fieldPath = Objects.requireNonNull(fieldPath);
        field.fieldType = fieldType;
        field.required = required;
        field.nullable = nullable;
        field.deprecated = deprecated;
        field.description = description;
        field.discoveredAt = Instant.now();
        return field;
    }
}
