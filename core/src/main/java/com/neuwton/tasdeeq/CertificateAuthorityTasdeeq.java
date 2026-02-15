package com.neuwton.tasdeeq;

import com.neuwton.tasdeeq.models.CertificateAuthorityTasdeeqResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CertificateAuthorityTasdeeq {

    private static final Logger logger = LoggerFactory.getLogger(CertificateAuthorityTasdeeq.class);

    private static CertificateAuthorityTasdeeqResult tasdeeq;

    public static CertificateAuthorityTasdeeqResult tasdeeq() {
        return tasdeeq;
    }

    static {
        try {
            // populate the default truststore's root CAs
            populateRootCAs();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private static void populateRootCAs() throws NoSuchAlgorithmException, KeyStoreException {
        Map<String, X509Certificate> rootCAsBySerialNumber = new HashMap<>();
        Map<String, X509Certificate> rootCAsBySubjectDN = new HashMap<>();
        // Get the default truststore using TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null); // Pass null to use the default truststore
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                X509TrustManager xtm = (X509TrustManager) tm;
                logger.info("Total number of Root CAs: {}", xtm.getAcceptedIssuers().length);

                for (X509Certificate x509 : xtm.getAcceptedIssuers()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Certificate: [");
                    sb.append(x509.getIssuerX500Principal().getName());
                    sb.append("] Serial Number: [");
                    sb.append(x509.getSerialNumber());
                    sb.append("] Issuer: [");
                    sb.append(x509.getIssuerDN());
                    sb.append("] Valid from: [");
                    sb.append(x509.getNotBefore());
                    sb.append("] Valid until: [");
                    sb.append(x509.getNotAfter());
                    sb.append("] Signature Algorithm: [");
                    sb.append(x509.getSigAlgName());
                    if (x509.getExtensionValue("2.5.29.19") != null) {
                        sb.append("] Basic Constraints: ");
                        sb.append(x509.getBasicConstraints());
                        if (isSelfSigned(x509) && x509.getBasicConstraints() != -1) {
                            rootCAsBySerialNumber.put(x509.getSerialNumber().toString(), x509);
                            rootCAsBySubjectDN.put(x509.getSubjectX500Principal().getName(), x509);
                            sb.append("] Certificate Type: ROOT CA");
                        } else {
                            sb.append("[ Certificate Type: INTERMEDIATE CA");
                        }
                    } else {
                        sb.append("] Certificate Type: LEAF");
                    }
                    logger.info(sb.toString());
                }
            }
        }
        tasdeeq = new CertificateAuthorityTasdeeqResult(rootCAsBySerialNumber, rootCAsBySubjectDN);
    }

    private static boolean rootCABySerialNumberExists(X509Certificate cert) {
        return tasdeeq().getRootCAsBySerialNumber().containsKey(cert.getSerialNumber().toString());
    }

    public static boolean rootCAisTrusted(List<X509Certificate> chain) {
        if (chain == null || chain.isEmpty()) {
            logger.error("Empty certificate chain provided");
            // TODO throw exception???
            return false;
        }

        // First: check if any cert in the chain is a trusted root
        for (X509Certificate cert : chain) {
            if (isSelfSigned(cert) && cert.getBasicConstraints() != -1) {
                if (rootCABySerialNumberExists(cert)) {
                    logger.info("Root CA [{}] found in chain and exists in truststore",
                            cert.getSubjectX500Principal().getName());
                    return true;
                }
            }
        }

        for (X509Certificate cert : chain) {
            if (cert.getBasicConstraints() != -1) {
                // intermediate CA cert — check if its issuer is a trusted root
                X509Certificate trustedRoot = tasdeeq().getRootCAsBySubjectDN().get(
                        cert.getIssuerX500Principal().getName());
                if (trustedRoot != null && verifySignature(cert, trustedRoot)) {
                    logger.info("Intermediate CA [{}] is signed by trusted Root CA [{}]",
                            cert.getSubjectX500Principal().getName(),
                            trustedRoot.getSubjectX500Principal().getName());
                    return true;
                }
            } else {
                // Leaf (end-entity) — check if directly signed by a trusted root
                X509Certificate trustedRoot = tasdeeq().getRootCAsBySubjectDN().get(
                        cert.getIssuerX500Principal().getName());
                if (trustedRoot != null && verifySignature(cert, trustedRoot)) {
                    logger.info("Leaf [{}] is directly signed by trusted Root CA [{}]",
                            cert.getSubjectX500Principal().getName(),
                            trustedRoot.getSubjectX500Principal().getName());
                    return true;
                } else if (isSelfSigned(cert)) {
                    // Self-signed leaf — rare/unusual, warn
                    logger.error("Self-signed leaf cert [{}] found — not tracing to any root CA",
                            cert.getSubjectX500Principal().getName());
                }
            }
        }

        logger.info("No certificate in the chain traces back to a trusted Root CA");
        return false;
    }

    private static boolean verifySignature(X509Certificate cert, X509Certificate issuer) {
        try {
            cert.verify(issuer.getPublicKey());
            return true;
        } catch (Exception e) {
            logger.warn("Signature verification failed for [{}] against issuer [{}]: {}",
                    cert.getSubjectX500Principal().getName(),
                    issuer.getSubjectX500Principal().getName(),
                    e.getMessage());
            return false;
        }
    }

    private static boolean isSelfSigned(X509Certificate cert) {
        try {
            cert.verify(cert.getPublicKey());
            return cert.getSubjectX500Principal().getName().equals(cert.getIssuerX500Principal().getName());
        } catch (Exception e) {
            return false;
        }
    }

}
