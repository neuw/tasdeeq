package com.neuwton.tasdeeq.models;

import java.time.Instant;
import java.util.List;

public class DNSTasdeeqResults {

    private List<DNSTasdeeqResult> results;
    private final Instant instant = Instant.now();

    public List<DNSTasdeeqResult> getResults() {
        return results;
    }

    public void setResults(List<DNSTasdeeqResult> results) {
        this.results = results;
    }

    public Instant getInstant() {
        return instant;
    }
}
