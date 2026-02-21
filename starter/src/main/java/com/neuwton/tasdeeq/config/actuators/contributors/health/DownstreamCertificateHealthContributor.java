package com.neuwton.tasdeeq.config.actuators.contributors.health;

import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import com.neuwton.tasdeeq.config.props.DownstreamCertTasdeeqProps;
import com.neuwton.tasdeeq.exceptions.CertificateValidationException;
import com.neuwton.tasdeeq.models.DownstreamCertResults;
import com.neuwton.tasdeeq.models.DownstreamCertTasdeeqResult;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DownstreamCertificateHealthContributor implements HealthIndicator {

    private static final long EXPIRY_WARNING_THRESHOLD_MS = 90L * 24 * 60 * 60 * 1000;
    private static final long EXPIRY_CRITICAL_THRESHOLD_MS = 15L * 24 * 60 * 60 * 1000;

    private volatile DownstreamCertResults results;
    private final DownstreamCertTasdeeqProps props;
    private final Object lock = new Object();

    public DownstreamCertificateHealthContributor(DownstreamCertResults results,
                                                  DownstreamCertTasdeeqProps props) {
        this.results = results;
        this.props = props;
    }

    @Override
    public Health health(boolean includeDetails) {
        if (CollectionUtils.isEmpty(props.getDomains())) {
            return Health.up()
                    .withDetail("reason", "no domains configured")
                    .build();
        }
        if (this.results == null ||
                this.results.getInstant().isBefore(Instant.now().minusSeconds(props.getCacheTtlSeconds()))) {
            synchronized (lock) {
                if (this.results == null ||
                        this.results.getInstant().isBefore(Instant.now().minusSeconds(props.getCacheTtlSeconds()))) {
                    this.results = downstreamCertTasdeeqResults(props);
                }
            }
        }
        return health();
    }

    @Override
    public Health health() {
        List<DownstreamCertTasdeeqResult> allResults = results.getResults();

        // partition results into buckets
        List<DownstreamCertTasdeeqResult> connectionFailures = allResults.stream()
                .filter(r -> r.getConnectionError() != null)
                .toList();

        List<DownstreamCertTasdeeqResult> untrusted = allResults.stream()
                .filter(r -> r.getConnectionError() == null)
                .filter(r -> r.isValidateChain() && !r.isTrusted())
                .toList();

        List<DownstreamCertTasdeeqResult> emptyChain = allResults.stream()
                .filter(r -> r.getConnectionError() == null)
                .filter(r -> CollectionUtils.isEmpty(r.getDownstreamCertChain()))
                .toList();

        List<String> expiredCerts = new ArrayList<>();
        List<String> expiringSoonCerts = new ArrayList<>();

        allResults.stream()
                .filter(r -> r.getConnectionError() == null)
                .filter(r -> !CollectionUtils.isEmpty(r.getDownstreamCertChain()))
                .forEach(r -> r.getDownstreamCertChain().forEach(cert -> {
                    long notAfterMs = cert.getNotAfter().toInstant().toEpochMilli();
                    long nowMs = System.currentTimeMillis();
                    String id = r.getHost() + ":" + r.getPort()
                            + " → " + cert.getSubjectX500Principal().getName();

                    if (notAfterMs < nowMs) {
                        // already expired → DOWN
                        expiredCerts.add(id + " [expired: " + cert.getNotAfter().toInstant() + "]");
                    } else if (notAfterMs < nowMs + EXPIRY_CRITICAL_THRESHOLD_MS) {
                        // expiring within 15 days → DOWN
                        expiredCerts.add(id + " [expires: " + cert.getNotAfter().toInstant() + "]");
                    } else if (notAfterMs < nowMs + EXPIRY_WARNING_THRESHOLD_MS) {
                        // expiring within 90 days → WARN
                        expiringSoonCerts.add(id + " [expires: " + cert.getNotAfter().toInstant() + "]");
                    }
                }));

        boolean isDown = !connectionFailures.isEmpty()
                || !emptyChain.isEmpty()
                || !expiredCerts.isEmpty();   // expired + expiring within 15 days both land here

        boolean isWarn = !isDown && (!expiringSoonCerts.isEmpty() || !untrusted.isEmpty());


        Health.Builder builder = isDown ? Health.down()
                : isWarn ? Health.status("WARN")
                : Health.up();

        if (!connectionFailures.isEmpty()) {
            builder.withDetail("connectionFailures", connectionFailures.stream()
                    .collect(Collectors.toMap(
                            r -> r.getHost() + ":" + r.getPort(),
                            DownstreamCertTasdeeqResult::getConnectionError)));
        }
        if (!untrusted.isEmpty()) {
            builder.withDetail("untrustedChains", untrusted.stream()
                    .map(r -> r.getHost() + ":" + r.getPort())
                    .collect(Collectors.toList()));
        }
        if (!emptyChain.isEmpty()) {
            builder.withDetail("emptyChains", emptyChain.stream()
                    .map(r -> r.getHost() + ":" + r.getPort())
                    .collect(Collectors.toList()));
        }
        if (!expiredCerts.isEmpty()) {
            builder.withDetail("expiredCerts", expiredCerts);
        }
        if (!expiringSoonCerts.isEmpty()) {
            builder.withDetail("expiringSoon", expiringSoonCerts);
        }

        builder.withDetail("lastFetched", results.getInstant().toString());
        builder.withDetail("nextRefreshAfter", results.getInstant()
                .plusSeconds(props.getCacheTtlSeconds()).toString());

        return builder.build();
    }

    private DownstreamCertResults downstreamCertTasdeeqResults(DownstreamCertTasdeeqProps props) {
        DownstreamCertResults downstreamCertResults = new DownstreamCertResults();
        List<DownstreamCertTasdeeqResult> results = new ArrayList<>();
        props.getDomains().forEach((k, v) -> {
            try {
                DownstreamCertTasdeeqResult result = DownstreamCertTasdeeq.tasdeeq(
                        v.getHost(), v.getPort(), v.isValidateChain());
                result.setHost(v.getHost())
                        .setPort(v.getPort())
                        .setValidateChain(v.isValidateChain());
                results.add(result);
            } catch (CertificateValidationException e) {
                results.add(new DownstreamCertTasdeeqResult()
                        .setHost(v.getHost())
                        .setPort(v.getPort())
                        .setValidateChain(v.isValidateChain())
                        .setTrusted(false)
                        .setConnectionError(e.getMessage()));
            }
        });
        downstreamCertResults.setResults(results);
        return downstreamCertResults;
    }
}