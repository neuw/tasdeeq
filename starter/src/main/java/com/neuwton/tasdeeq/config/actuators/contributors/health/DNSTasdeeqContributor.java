package com.neuwton.tasdeeq.config.actuators.contributors.health;

import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.config.props.DNSTasdeeqProps;
import com.neuwton.tasdeeq.models.DNSTasdeeqResult;
import com.neuwton.tasdeeq.models.DNSTasdeeqResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.event.EventListener;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DNSTasdeeqContributor implements HealthIndicator {

    private volatile DNSTasdeeqResults results;
    private final JsonMapper jsonMapper;
    private final DNSTasdeeqProps props;
    private final AtomicBoolean fetching = new AtomicBoolean(false);
    private volatile boolean applicationReady = false;

    private static final Logger logger = LoggerFactory.getLogger(DNSTasdeeqContributor.class);

    public DNSTasdeeqContributor(DNSTasdeeqResults results,
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
            CompletableFuture.runAsync(() -> {
                try {
                    this.results = DNSTasdeeq.tasdeeq(
                            props.getDomains(),
                            props.getRecords().toArray(new String[0]));
                } finally {
                    fetching.set(false);
                }
            });
        }
    }

    @Override
    public Health health(boolean includeDetails) {
        if (CollectionUtils.isEmpty(props.getDomains())) {
            return Health.up()
                    .withDetail("reason", "no domains configured")
                    .build();
        }

        if (!applicationReady || this.results == null) {
            return Health.up()
                    .withDetail("reason", applicationReady ? "pending first fetch" : "application starting up")
                    .build();
        }

        if (isStale()) {
            // stale — block and fetch fresh data
            if (fetching.compareAndSet(false, true)) {
                try {
                    this.results = DNSTasdeeq.tasdeeq(
                            props.getDomains(),
                            props.getRecords().toArray(new String[0]));
                } finally {
                    fetching.set(false);
                }
            }
        }

        return health();
    }

    private boolean isStale() {
        return this.results.getInstant()
                .isBefore(Instant.now().minusSeconds(props.getCacheTtlSeconds()));
    }

    @Override
    public Health health() {
        if (CollectionUtils.isEmpty(results.getResults())) {
            return Health.up()
                    .withDetail("reason", "no data available yet")
                    .build();
        }
        List<DNSTasdeeqResult> failed = results.getResults().stream()
                .filter(r -> !r.getConnectionError().equals("no error"))
                .toList();

        List<DNSTasdeeqResult> noRecords = results.getResults().stream()
                .filter(r -> r.getConnectionError().equals("no error"))
                .filter(r -> r.getRecords().isEmpty())
                .toList();

        if (failed.size() == results.getResults().size()) {
            // all domains failed
            return Health.down()
                    .withDetail("reason", "All domains have connection errors")
                    .withDetail("failed", failed.stream()
                            .collect(Collectors.toMap(
                                    DNSTasdeeqResult::getDomain,
                                    DNSTasdeeqResult::getConnectionError
                            )))
                    .build();
        }

        if (!failed.isEmpty()) {
            // some failed, some ok
            return Health.status("WARN")
                    .withDetail("reason", "Some domains have connection errors")
                    .withDetail("failed", failed.stream()
                            .collect(Collectors.toMap(
                                    DNSTasdeeqResult::getDomain,
                                    DNSTasdeeqResult::getConnectionError
                            )))
                    .withDetail("healthy", results.getResults().size() - failed.size())
                    .build();
        }

        if (!noRecords.isEmpty()) {
            return Health.status("WARN")
                    .withDetail("reason", "Some domains returned no records")
                    .withDetail("domains", noRecords.stream()
                            .map(DNSTasdeeqResult::getDomain)
                            .collect(Collectors.toList()))
                    .build();
        }

        Map<String, Object> domainMap = results.getResults().stream()
                .collect(Collectors.toMap(
                        DNSTasdeeqResult::getDomain,
                        jsonMapper::valueToTree,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return Health.up()
                .withDetails(domainMap)
                .build();
    }
}