package com.neuwton.tasdeeq.config.actuators.contributors.info;

import com.neuwton.tasdeeq.config.actuators.contributors.models.CACertDetails;
import com.neuwton.tasdeeq.models.CertificateAuthorityTasdeeqResult;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.neuwton.tasdeeq.CertificateAuthorityTasdeeq.getCertificateType;

public class CertificateAuthorityContributor implements InfoContributor {

    private final CertificateAuthorityTasdeeqResult result;

    public CertificateAuthorityContributor(CertificateAuthorityTasdeeqResult result) {
        this.result = result;
    }

    @Override
    public void contribute(Info.Builder builder) {
        List<CACertDetails> allCerts = buildCertDetails(result.getRootCAsBySubjectDN());

        List<CACertDetails> expiredList = allCerts.stream()
                .filter(this::isExpired)
                .peek(cert -> cert.setStatus("EXPIRED"))
                .collect(Collectors.toList());

        List<CACertDetails> expiringSoonList = allCerts.stream()
                .filter(this::isExpiringSoon)
                .peek(cert -> cert.setStatus("EXPIRING_SOON"))
                .collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", allCerts.size());
        summary.put("expired", expiredList.size());
        summary.put("expiringSoon", expiringSoonList.size());
        summary.put("algorithms", allCerts.stream()
                .collect(Collectors.groupingBy(
                        CACertDetails::getSignatureAlgorithm,
                        Collectors.counting())));

        builder.withDetail("trusted-roots-summary", summary);
        builder.withDetail("trusted-roots", allCerts);

        if (!expiredList.isEmpty()) {
            builder.withDetail("trusted-roots-expired", expiredList);
        }
        if (!expiringSoonList.isEmpty()) {
            builder.withDetail("trusted-roots-expiring-soon", expiringSoonList);
        }
    }

    private List<CACertDetails> buildCertDetails(Map<String, X509Certificate> source) {
        return source.entrySet().stream()
                .map(entry -> {
                    X509Certificate cert = entry.getValue();
                    return new CACertDetails()
                            .setCertificateName(entry.getKey())
                            .setSerialNumber(cert.getSerialNumber().toString())
                            .setIssuerDN(cert.getIssuerX500Principal().getName())
                            .setValidFrom(cert.getNotBefore().toInstant().toString())
                            .setValidFromEpochMs(cert.getNotBefore().toInstant().toEpochMilli())
                            .setValidUntil(cert.getNotAfter().toInstant().toString())
                            .setValidUntilEpochMs(cert.getNotAfter().toInstant().toEpochMilli())
                            .setSignatureAlgorithm(cert.getSigAlgName())
                            .setBasicConstraints(resolveBasicConstraints(cert.getBasicConstraints()))
                            .setCertificateType(getCertificateType(cert));
                })
                .collect(Collectors.toList());
    }

    private static final long EXPIRY_WARNING_THRESHOLD_MS = 90L * 24 * 60 * 60 * 1000;

    private boolean isExpired(CACertDetails cert) {
        return cert.getValidUntilEpochMs() != null &&
                cert.getValidUntilEpochMs() < System.currentTimeMillis();
    }

    private boolean isExpiringSoon(CACertDetails cert) {
        if (cert.getValidUntilEpochMs() == null) return false;
        long nowMillis = System.currentTimeMillis();
        long notAfterEpochMs = cert.getValidUntilEpochMs();
        return notAfterEpochMs > nowMillis &&
                notAfterEpochMs < nowMillis + EXPIRY_WARNING_THRESHOLD_MS;
    }

    private String resolveBasicConstraints(int value) {
        if (value == Integer.MAX_VALUE) return "unlimited";
        if (value == -1) return "not-a-ca";
        return "max-path-length:" + value;
    }
}
