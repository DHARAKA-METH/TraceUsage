package com.traceusage.traceusage.apikey.repository;

import com.traceusage.traceusage.apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
}
