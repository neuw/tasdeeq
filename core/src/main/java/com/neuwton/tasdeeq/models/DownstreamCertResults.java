package com.neuwton.tasdeeq.models;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class DownstreamCertResults {

    private List<DownstreamCertTasdeeqResult> results = Collections.emptyList();
    private Instant instant;

    public DownstreamCertResults() {
        this.instant = Instant.now();
    }

    public List<DownstreamCertTasdeeqResult> getResults() {
        return results;
    }

    public DownstreamCertResults setResults(List<DownstreamCertTasdeeqResult> results) {
        this.results = results;
        this.instant = Instant.now();
        return this;
    }

    public Instant getInstant() {
        return instant;
    }
}
