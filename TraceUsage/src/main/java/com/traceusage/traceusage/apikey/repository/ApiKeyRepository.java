package com.traceusage.traceusage.apikey.repository;

import com.traceusage.traceusage.apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    @EntityGraph(attributePaths = "application")
    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    @EntityGraph(attributePaths = "application")
    Optional<ApiKey> findByKeyHashAndKeyTypeAndRevokedAtIsNull(String keyHash, String keyType);
}
