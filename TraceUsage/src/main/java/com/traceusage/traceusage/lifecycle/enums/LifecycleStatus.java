package com.traceusage.traceusage.lifecycle.enums;

/**
 * Evaluation result for an API endpoint. These are observations, not commands.
 */
public enum LifecycleStatus {

    /** Not enough reliable monitoring history to draw a conclusion. */
    NEWLY_MONITORED,

    /** Not deprecated, with observed requests inside the inactivity threshold. */
    ACTIVE,

    /** Not deprecated, with no observed requests inside the inactivity threshold. */
    INACTIVE,

    /** Deprecated, but still receiving observed requests. */
    DEPRECATED_ACTIVE,

    /** Deprecated and inactive, but not yet eligible for removal review. */
    DEPRECATED_INACTIVE,

    /**
     * Deprecated and inactive, and all evidence gates are satisfied.
     * This means "eligible for a manual removal review", not "safe to delete".
     */
    REMOVAL_CANDIDATE
}