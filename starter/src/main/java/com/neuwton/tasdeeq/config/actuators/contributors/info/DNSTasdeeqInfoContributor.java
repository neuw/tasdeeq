package com.neuwton.tasdeeq.config.actuators.contributors.info;

import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.config.props.DNSTasdeeqProps;
import com.neuwton.tasdeeq.models.DNSTasdeeqResult;
import com.neuwton.tasdeeq.models.DNSTasdeeqResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DNSTasdeeqInfoContributor implements InfoContributor {

    private static final Logger logger = LoggerFactory.getLogger(DNSTasdeeqInfoContributor.class);

    private volatile DNSTasdeeqResults results;
    private final JsonMapper jsonMapper;
    private final DNSTasdeeqProps props;

    public DNSTasdeeqInfoContributor(DNSTasdeeqResults results,
                                     DNSTasdeeqProps props,
                                     JsonMapper jsonMapper) {
        this.results = results;
        this.props = props;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void contribute(Info.Builder builder) {
        if (CollectionUtils.isEmpty(props.getDomains())) {
            builder.withDetail("dns-records", "no domains configured");
            return;
        }

        if (this.results == null || CollectionUtils.isEmpty(this.results.getResults())) {
            builder.withDetail("dns-records", "application starting up");
            return;
        }

        if (isStale()) {
            results = DNSTasdeeq.tasdeeq(
                    props.getDomains(),
                    props.getRecords().toArray(new String[0]));
        }

        Map<String, Object> dnsInfo = new LinkedHashMap<String, Object>();
        
        Map<String, Object> domainMap = this.results.getResults().stream()
                .collect(Collectors.toMap(
                        DNSTasdeeqResult::getDomain,
                        jsonMapper::valueToTree,
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