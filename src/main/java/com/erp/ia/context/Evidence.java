package com.erp.ia.context;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Typed evidence: what data was consulted, from where, when, and what was
 * returned.
 */
public record Evidence(
        String source, // e.g. "StockQueryTool", "ProductQueryTool"
        String query, // what was asked
        Instant timestamp,
        @Schema(implementation = Object.class, description = "Tool result payload (varies by tool)") Object payload // typed
                                                                                                                    // data
                                                                                                                    // returned
) {
    public Evidence(String source, String query, Object payload) {
        this(source, query, Instant.now(), payload);
    }
}
