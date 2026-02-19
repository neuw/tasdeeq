package com.neuwton.tasdeeq.config.actuators.contributors.info;

import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.config.props.DNSTasdeeqProps;
import com.neuwton.tasdeeq.models.DNSTasdeeqResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DNSTasdeeqInfoContributor implements InfoContributor {

    private volatile DNSTasdeeqResults results;
    private final JsonMapper jsonMapper;
    private final DNSTasdeeqProps props;
    private final AtomicBoolean fetching = new AtomicBoolean(false);
    private volatile boolean applicationReady = false;

    private static final Logger logger = LoggerFactory.getLogger(DNSTasdeeqInfoContributor.class);

    public DNSTasdeeqInfoContributor(DNSTasdeeqResults results,
                                     DNSTasdeeqProps props,
                                     JsonMapper jsonMapper) {
        this.results = results;
        this.props = props;
        this.jsonMapper = jsonMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        applicationReady = true;
        // Trigger first fetch async after boot
        if (fetching.compareAndSet(false, true)) {
            CompletableFuture.runAsync(new Runnable() {
                public void run() {
                    try {
                        results = DNSTasdeeq.tasdeeq(
                                props.getDomains(),
                                props.getRecords().toArray(new String[0]));
                    } finally {
                        fetching.set(false);
                    }
                }
            });
        }
    }

    @Override
    public void contribute(Info.Builder builder) {
        if (CollectionUtils.isEmpty(props.getDomains())) {
            builder.withDetail("dns-records", "no domains configured");
            return;
        }

        if (!applicationReady || this.results == null || CollectionUtils.isEmpty(this.results.getResults())) {
            builder.withDetail("dns-records", applicationReady ? "pending first fetch" : "application starting up");
            return;
        }

        if (isStale()) {
            // stale — trigger async refresh, serve stale data
            if (fetching.compareAndSet(false, true)) {
                CompletableFuture.runAsync(new Runnable() {
                    public void run() {
                        try {
                            results = DNSTasdeeq.tasdeeq(
                                    props.getDomains(),
                                    props.getRecords().toArray(new String[0]));
                        } finally {
                            fetching.set(false);
                        }
                    }
                });
            }
        }

        Map<String, Object> dnsInfo = new LinkedHashMap<String, Object>();
        
        Map<String, Object> domainMap = this.results.getResults().stream()
                .collect(Collectors.toMap(
                        r -> r.getDomain(),
                        r -> jsonMapper.valueToTree(r),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        
        dnsInfo.put("domains", domainMap);
        dnsInfo.put("fetchedAt", this.results.getInstant().toString());
        dnsInfo.put("nextRefreshAfter", this.results.getInstant()
                .plusSeconds(props.getCacheTtlSeconds()).toString());
        
        builder.withDetail("dns-records", dnsInfo);
    }

    private boolean isStale() {
        return this.results.getInstant()
                .isBefore(Instant.now().minusSeconds(props.getCacheTtlSeconds()));
    }
}