package com.neuwton.tasdeeq;

import com.neuwton.tasdeeq.exceptions.CertificateValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class DownstreamCertTasdeeq {

    private static final Logger logger = LoggerFactory.getLogger(DownstreamCertTasdeeq.class);

    /**
     * Fetches the downstream server certificate with chain validation enabled by default.
     *
     * @param hostName the target hostname
     * @return the server's X509 certificate
     * @throws CertificateValidationException if validation fails
     */
    public static List<X509Certificate> getDownstreamCert(String hostName) {
        return getDownstreamCert(hostName, 443, true);
    }

    public static List<X509Certificate> getDownstreamCert(String hostName, int port) {
        return getDownstreamCert(hostName, port, true);
    }

    /**
     * Fetches the downstream server certificate.
     *
     * @param hostName the target hostname
     * @param port the target port
     * @param validateChain if true, strictly validates the certificate chain (fails on invalid certs).
     *                      if false, attempts with validation first, then retries without validation on failure.
     * @return the server's X509 certificate
     * @throws CertificateValidationException if validateChain=true and validation fails
     */
    public static List<X509Certificate> getDownstreamCert(String hostName, int port, boolean validateChain) {
        logger.debug("Fetching certificate for {}:{} (validateChain={})", hostName, port, validateChain);

        if (validateChain) {
            // Strict mode: fail hard if validation fails
            try {
                return fetchCertificateWithValidation(hostName, port);
            } catch (Exception e) {
                logger.error("Certificate validation failed for {}:{}", hostName, port, e);
                throw new CertificateValidationException(
                        "Certificate chain validation failed for " + hostName + ":" + port, e);
            }
        } else {
            // Lenient mode: try with validation first, fallback to without validation
            try {
                logger.info("Attempting the fetching of certificate details with validation 'ON' first for {}:{}", hostName, port);
                return fetchCertificateWithValidation(hostName, port);
            } catch (Exception e) {
                logger.error("Validation failed for {}:{}, retrying next without chain validation: {}",
                        hostName, port, e.getMessage());

                return fetchCertificateWithoutValidation(hostName, port);
            }
        }
    }

    /**
     * Fetches certificate WITH validation enabled (secure).
     */
    private static List<X509Certificate> fetchCertificateWithValidation(String hostname, int port) throws Exception {
        logger.info("Fetching certificate with validation enabled");
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(hostname, port)) {
            socket.startHandshake();
            SSLSession session = socket.getSession();

            Certificate[] certs = session.getPeerCertificates();
            List<X509Certificate> x509Certs = new ArrayList<>();

            logger.info("Retrieved {} certificate(s) in chain", certs.length);

            for (Certificate cert : certs) {
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    x509Certs.add(x509);
                    String certType = classifyCertificate(x509);
                    logCertificateDetails(x509, certType);
                }
            }

            // well it will be ordered 99.999999% of the times :-), just being extra sure.
            return orderChain(x509Certs);
        }
    }

    /**
     * Fetches certificate WITHOUT validation (for self-signed/expired certs).
     * IMPORTANT: This bypasses security checks - use only when appropriate!
     */
    private static List<X509Certificate> fetchCertificateWithoutValidation(String hostname, int port) {
        logger.info("Fetching certificate WITHOUT validation for {}:{} - this bypasses security!", hostname, port);

        // Store original settings
        SSLSocketFactory originalSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        try {
            disableCertificateValidation();

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(hostname, port)) {
                socket.startHandshake();
                SSLSession session = socket.getSession();

                Certificate[] certs = session.getPeerCertificates();
                List<X509Certificate> x509Certs = new ArrayList<>();

                logger.info("Retrieved {} certificate(s) in chain", certs.length);

                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate) {
                        X509Certificate x509 = (X509Certificate) cert;
                        x509Certs.add(x509);
                        logCertificateDetails(x509, classifyCertificate(x509));
                    }
                }

                return orderChain(x509Certs);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch certificate even without validation", e);
            throw new CertificateValidationException(
                    "Failed to fetch certificate for " + hostname + ":" + port, e);
        } finally {
            try {
                enableCertificateValidation(originalSocketFactory, originalHostnameVerifier);
                logger.info("Certificate validation re-enabled");
            } catch (Exception e) {
                logger.error("Failed to re-enable certificate validation!", e);
            }
        }
    }

    /**
     * Fetches a full certificate chain and prints detailed information.
     */
    public static List<X509Certificate> fetchCertificateChain(String hostname, int port, boolean validateChain) {
        logger.debug("Fetching certificate chain for {}:{}", hostname, port);

        SSLSocketFactory originalSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        try {
            if (!validateChain) {
                disableCertificateValidation();
            }

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(hostname, port)) {
                socket.startHandshake();
                SSLSession session = socket.getSession();

                Certificate[] certs = session.getPeerCertificates();
                List<X509Certificate> x509Certs = new ArrayList<>();

                logger.info("Retrieved {} certificate(s) in chain", certs.length);

                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate) {
                        X509Certificate x509 = (X509Certificate) cert;
                        x509Certs.add(x509);
                        logCertificateDetails(x509, classifyCertificate(x509));
                    }
                }

                return orderChain(x509Certs);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch certificate chain", e);
            throw new CertificateValidationException(
                    "Failed to fetch certificate chain for " + hostname + ":" + port, e);
        } finally {
            if (!validateChain) {
                try {
                    enableCertificateValidation(originalSocketFactory, originalHostnameVerifier);
                } catch (Exception e) {
                    logger.error("Failed to re-enable certificate validation!", e);
                }
            }
        }
    }

    private static void logCertificateDetails(X509Certificate x509, String certType) {
        try {
            logger.info("certificate details: Subject: [{}], Issuer: [{}], Validity: [{}] to [{}], Serial: [{}], type: [{}], EKU: {}", x509.getSubjectDN(), x509.getIssuerDN(), x509.getNotBefore(), x509.getNotAfter(), x509.getSerialNumber(), certType, getExtendedKeyUsage(x509));
        } catch (Exception e) {
            logger.error("Error logging certificate details", e);
        }
    }

    private static List<X509Certificate> orderChain(List<X509Certificate> certs) {
        // Build a map: subject -> certificate
        Map<Principal, X509Certificate> bySubject = new HashMap<>();
        Set<Principal> issuers = new HashSet<>();

        for (X509Certificate cert : certs) {
            bySubject.put(cert.getSubjectX500Principal(), cert);
            issuers.add(cert.getIssuerX500Principal());
        }

        // Find the leaf: a cert whose subject is NOT an issuer of any other cert
        X509Certificate leaf = certs.stream()
                .filter(c -> !issuers.contains(c.getSubjectX500Principal())
                        || c.getSubjectX500Principal().equals(c.getIssuerX500Principal()))
                .filter(c -> !c.getSubjectX500Principal().equals(c.getIssuerX500Principal()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find leaf certificate"));

        // Walk up the chain
        List<X509Certificate> ordered = new ArrayList<>();
        X509Certificate current = leaf;
        Set<Principal> visited = new HashSet<>();

        while (current != null && visited.add(current.getSubjectX500Principal())) {
            ordered.add(current);
            X509Certificate issuer = bySubject.get(current.getIssuerX500Principal());
            if (issuer == current) break; // self-signed root
            current = issuer;
        }

        return ordered;
    }

    /**
     * Converts X509Certificate to PEM format.
     */
    public static String toPEM(X509Certificate cert) {
        try {
            byte[] encoded = cert.getEncoded();
            return "-----BEGIN CERTIFICATE-----\n" +
                    Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded) +
                    "\n-----END CERTIFICATE-----";
        } catch (Exception e) {
            logger.error("Failed to convert certificate to PEM", e);
            throw new RuntimeException("Failed to convert certificate to PEM", e);
        }
    }

    private static void disableCertificateValidation() throws Exception {
        logger.warn("DISABLING certificate validation, only to fetch certificates, insecure mode is risky");

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLContext.setDefault(sc);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private static void enableCertificateValidation(
            SSLSocketFactory originalSocketFactory,
            HostnameVerifier originalHostnameVerifier) throws Exception {

        logger.debug("Re-enabling certificate validation");

        // Restore original settings
        HttpsURLConnection.setDefaultSSLSocketFactory(originalSocketFactory);
        HttpsURLConnection.setDefaultHostnameVerifier(originalHostnameVerifier);

        // Reset SSLContext to default
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, null, null);
        SSLContext.setDefault(sc);
    }

    private static String classifyCertificate(X509Certificate cert) {
        boolean isCA = cert.getBasicConstraints() != -1;
        boolean isSelfSigned = isSelfSigned(cert);

        if (isCA) {
            return isSelfSigned ? "ROOT CA" : "INTERMEDIATE CA";
        }
        return "LEAF [End-Entity]";
    }

    private static final Map<String, String> EKU_DESCRIPTIONS = new HashMap<>();

    static {
        EKU_DESCRIPTIONS.put("1.3.6.1.5.5.7.3.1", "TLS Web Server Authentication");
        EKU_DESCRIPTIONS.put("1.3.6.1.5.5.7.3.2", "TLS Web Client Authentication");
        EKU_DESCRIPTIONS.put("1.3.6.1.5.5.7.3.3", "Code Signing");
        EKU_DESCRIPTIONS.put("1.3.6.1.5.5.7.3.4", "Email Protection");
        EKU_DESCRIPTIONS.put("1.3.6.1.5.5.7.3.8", "Time Stamping");
    }

    private static String getExtendedKeyUsage(X509Certificate cert) throws CertificateParsingException {
        StringBuilder result = new StringBuilder("Extended Key Usage:");
        List<String> ekuOIDs = cert.getExtendedKeyUsage();

        if (ekuOIDs != null) {
            for (String oid : ekuOIDs) {
                String usage = EKU_DESCRIPTIONS.getOrDefault(oid, "Unknown (" + oid + ")");
                result.append(" [").append(usage).append("]");
            }
        } else {
            result.append(" [None]");
        }
        return result.toString();
    }

    private static boolean isSelfSigned(X509Certificate cert) {
        try {
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
        } catch (Exception e) {
            logger.trace("Certificate {} is not self-signed", cert.getSubjectX500Principal().getName());
            return false;
        }
    }

    public static void main(String[] args) {
        /*List<X509Certificate> certs = getDownstreamCert("private-tapi.telstra.com", 443, true);
        if (CertificateAuthorityTasdeeq.rootCAisTrusted(certs)) {
            logger.info("Root CA is trusted for private-tapi.telstra.com");
        }
        if (CertificateAuthorityTasdeeq.rootCAisTrusted(getDownstreamCert("ca-mini.in.01101011.in", 443, false))) {
            logger.info("Root CA is trusted for ca-mini.in.01101011.in");
        } else {
            logger.warn("Root CA is NOT trusted for ca-mini.in.01101011.in");
        }
        */
        if (CertificateAuthorityTasdeeq.rootCAisTrusted(getDownstreamCert("private-tapi.telstra.com", 443, true))) {
            logger.info("Root CA is trusted for private-tapi.telstra.com");
        } else {
            logger.warn("Root CA is NOT trusted for private-tapi.telstra.com");
        }
        if (CertificateAuthorityTasdeeq.rootCAisTrusted(getDownstreamCert("speedtest.telstra.com", 443, true))) {
            logger.info("Root CA is trusted for speedtest.telstra.com");
        } else {
            logger.warn("Root CA is NOT trusted for speedtest.telstra.com");
        }
        try {
            List<X509Certificate> certs = getDownstreamCert("ca-mini.in.01101011.in", 443, false);
            if (CertificateAuthorityTasdeeq.rootCAisTrusted(certs)) {
                logger.info("Root CA is trusted for ca-mini.in.01101011.in");
            } else {
                logger.warn("Root CA is NOT trusted for ca-mini.in.01101011.in");
            }
        } catch (CertificateValidationException e) {
            logger.warn("Failed to validate certificate for ca-mini.in.01101011.in: {}", e.getMessage());
        }
    }
}