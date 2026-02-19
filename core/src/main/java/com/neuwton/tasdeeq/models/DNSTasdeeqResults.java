package com.neuwton.tasdeeq.models;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class DNSTasdeeqResults {

    private List<DNSTasdeeqResult> results = Collections.emptyList();
    private final Instant instant = Instant.now();

    public List<DNSTasdeeqResult> getResults() {
        return results;
    }

    public DNSTasdeeqResults setResults(List<DNSTasdeeqResult> results) {
        this.results = results;
        return this;
    }

    public Instant getInstant() {
        return instant;
    }
}
