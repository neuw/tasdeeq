package com.neuwton.tasdeeq.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "neuwton.tasdeeq.dns")
public class DNSTasdeeqProps {

    private boolean enabled = true;
    private int cacheTtlSeconds = 3600;
    private List<String> domains = new ArrayList<>();
    private List<String> records = List.of("A", "AAAA", "MX", "TXT", "CNAME", "NS");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public List<String> getRecords() {
        return records;
    }

    public void setRecords(List<String> records) {
        this.records = records;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public DNSTasdeeqProps setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
        return this;
    }
}
