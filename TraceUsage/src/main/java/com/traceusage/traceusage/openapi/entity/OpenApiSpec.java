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
@Table(name = "openapi_specs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenApiSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "document_hash", nullable = false, length = 128)
    private String documentHash;

    @Column(name = "raw_document", nullable = false, columnDefinition = "TEXT")
    private String rawDocument;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    public static OpenApiSpec create(Application application,
                                     String fileName,
                                     String contentType,
                                     String documentHash,
                                     String rawDocument) {
        OpenApiSpec spec = new OpenApiSpec();
        spec.application = Objects.requireNonNull(application);
        spec.fileName = Objects.requireNonNull(fileName);
        spec.contentType = contentType;
        spec.documentHash = Objects.requireNonNull(documentHash);
        spec.rawDocument = Objects.requireNonNull(rawDocument);
        spec.importedAt = Instant.now();
        return spec;
    }
}
