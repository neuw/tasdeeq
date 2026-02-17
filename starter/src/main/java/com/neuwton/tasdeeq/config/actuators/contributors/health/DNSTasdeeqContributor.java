package com.neuwton.tasdeeq.config.actuators.contributors.health;

import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.config.props.DNSTasdeeqProps;
import com.neuwton.tasdeeq.models.DNSTasdeeqResult;
import com.neuwton.tasdeeq.models.DNSTasdeeqResults;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.CollectionUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DNSTasdeeqContributor implements HealthIndicator {

    private DNSTasdeeqResults results;
    private final JsonMapper jsonMapper;
    private final DNSTasdeeqProps props;

    public DNSTasdeeqContributor(DNSTasdeeqResults results,
                                 DNSTasdeeqProps props,
                                 JsonMapper jsonMapper) {
        this.results = results;
        this.props = props;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Health health(boolean includeDetails) {
        if (!CollectionUtils.isEmpty(props.getDomains())) {
            if (this.results.getInstant().isBefore(Instant.now().minusSeconds(props.getCacheTtlSeconds()))) {
                // fetch new results once 10 minutes have lapsed since the last fetch
                this.results = DNSTasdeeq.tasdeeq(
                        props.getDomains(),
                        props.getRecords().toArray(String[]::new));
            }
            return health();
        } else {
            return Health.up()
                    .withDetail("reason", "no domains configured")
                    .build();
        }
    }

    @Override
    public Health health() {
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
