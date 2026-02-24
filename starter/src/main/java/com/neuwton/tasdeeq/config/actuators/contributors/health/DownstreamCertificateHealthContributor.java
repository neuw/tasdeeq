package com.neuwton.tasdeeq.config.actuators.contributors.health;

import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import com.neuwton.tasdeeq.config.props.DownstreamCertTasdeeqProps;
import com.neuwton.tasdeeq.exceptions.CertificateValidationException;
import com.neuwton.tasdeeq.models.DownstreamCertResults;
import com.neuwton.tasdeeq.models.DownstreamCertTasdeeqResult;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.CollectionUtils;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DownstreamCertificateHealthContributor implements HealthIndicator {

    private static final long EXPIRY_WARNING_THRESHOLD_MS = 45L * 24 * 60 * 60 * 1000;
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
                        expiredCerts.add(id + " [expired: " + cert.getNotAfter().toInstant() + "]");
                    } else if (notAfterMs < nowMs + EXPIRY_CRITICAL_THRESHOLD_MS) {
                        expiredCerts.add(id + " [expires: " + cert.getNotAfter().toInstant() + "]");
                    } else if (notAfterMs < nowMs + EXPIRY_WARNING_THRESHOLD_MS) {
                        expiringSoonCerts.add(id + " [expires: " + cert.getNotAfter().toInstant() + "]");
                    }
                }));

        boolean isDown = !connectionFailures.isEmpty()
                || !emptyChain.isEmpty()
                || !expiredCerts.isEmpty();

        boolean isWarn = !isDown && (!expiringSoonCerts.isEmpty() || !untrusted.isEmpty());

        Health.Builder builder = isDown ? Health.down()
                : isWarn ? Health.status("WARN")
                : Health.up();

        if (isWarn) {
            builder.withDetail("warningReason", "some certificates are expiring soon, or untrusted");
        }

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

        // Add all results with serializable cert info
        List<Map<String, Object>> allResultsDetails = allResults.stream()
                .map(this::buildResultDetails)
                .collect(Collectors.toList());

        builder.withDetail("allResults", allResultsDetails);
        builder.withDetail("lastFetched", results.getInstant().toString());
        builder.withDetail("nextRefreshAllowedAfter", results.getInstant().plusSeconds(props.getCacheTtlSeconds()));

        return builder.build();
    }

    private Map<String, Object> buildResultDetails(DownstreamCertTasdeeqResult result) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("host", result.getHost());
        details.put("port", result.getPort());
        details.put("trusted", result.isTrusted());
        details.put("validateChain", result.isValidateChain());

        if (result.getConnectionError() != null) {
            details.put("connectionError", result.getConnectionError());
        }

        if (!CollectionUtils.isEmpty(result.getDownstreamCertChain())) {
            List<Map<String, Object>> certChain = result.getDownstreamCertChain().stream()
                    .map(this::buildCertInfo)
                    .collect(Collectors.toList());
            details.put("certificateChain", certChain);
        }

        return details;
    }

    private Map<String, Object> buildCertInfo(X509Certificate cert) {
        Map<String, Object> certInfo = new LinkedHashMap<>();
        certInfo.put("subject", cert.getSubjectX500Principal().getName());
        certInfo.put("issuer", cert.getIssuerX500Principal().getName());
        certInfo.put("serialNumber", cert.getSerialNumber().toString());
        certInfo.put("notBefore", cert.getNotBefore().toInstant().toString());
        certInfo.put("notBeforeEpochMs", cert.getNotBefore().toInstant().toEpochMilli());
        certInfo.put("notAfter", cert.getNotAfter().toInstant().toString());
        certInfo.put("notAfterEpochMs", cert.getNotAfter().toInstant().toEpochMilli());
        certInfo.put("signatureAlgorithm", cert.getSigAlgName());
        certInfo.put("version", cert.getVersion());

        // Basic constraints
        int basicConstraints = cert.getBasicConstraints();
        if (basicConstraints == -1) {
            certInfo.put("basicConstraints", "not-a-ca");
        } else if (basicConstraints == Integer.MAX_VALUE) {
            certInfo.put("basicConstraints", "unlimited");
        } else {
            certInfo.put("basicConstraints", "max-path-length:" + basicConstraints);
        }

        // Certificate type
        boolean isCA = cert.getBasicConstraints() != -1;
        boolean isSelfSigned = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
        if (isCA) {
            certInfo.put("type", isSelfSigned ? "ROOT_CA" : "INTERMEDIATE_CA");
        } else {
            certInfo.put("type", "LEAF");
        }

        return certInfo;
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