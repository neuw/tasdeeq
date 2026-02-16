package com.neuwton.tasdeeq;

import com.neuwton.tasdeeq.exceptions.CertificateValidationException;
import com.neuwton.tasdeeq.models.DownstreamCertTasdeeqResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
    public static DownstreamCertTasdeeqResult tasdeeq(String hostName) throws NoSuchAlgorithmException, KeyManagementException {
        return tasdeeq(hostName, 443, true);
    }

    public static DownstreamCertTasdeeqResult tasdeeq(String hostName, int port) throws NoSuchAlgorithmException, KeyManagementException {
        return tasdeeq(hostName, port, true);
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
    public static DownstreamCertTasdeeqResult tasdeeq(String hostName, int port, boolean validateChain) {
        logger.info("Fetching certificate for {}:{} (validateChain={})", hostName, port, validateChain);

        DownstreamCertTasdeeqResult result = new DownstreamCertTasdeeqResult();

        if (validateChain) {
            // Strict mode: fail hard if validation fails
            try {
                result.setDownstreamCertChain(fetchCertificates(hostName, port));
            } catch (IOException e) {
                result.setTrusted(false);
                logger.error("Certificate validation failed for {}:{}", hostName, port, e);
                throw new CertificateValidationException(
                        "Certificate chain validation failed for " + hostName + ":" + port, e);
            }
        } else {
            // Lenient mode: try with validation first, fallback to without validation
            try {
                logger.info("Attempting the fetching of certificate details with validation 'ON' first for {}:{}", hostName, port);
                result.setDownstreamCertChain(fetchCertificates(hostName, port));
            } catch (IOException e) {
                logger.error("Validation failed for {}:{}, retrying next without chain validation: {}",
                        hostName, port, e.getMessage());
                result.setTrusted(false);
                try {
                    result.setDownstreamCertChain(fetchCertificateWithoutChainValidation(hostName, port));
                } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                    logger.error("Failed to fetch certificate without validation for hostname: {} on port: {}, exception is {}", hostName, port, ex.getMessage());
                }
            }
        }
        return result;
    }

    private static List<X509Certificate> fetchCertificates(String hostname, int port) throws IOException {
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
     * Fetches with custom truststore.
     */
    public static List<X509Certificate> getDownstreamCertWithCustomTruststore(
            String hostname, int port, X509Certificate... additionalCAs) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, KeyManagementException {

        logger.info("Fetching certificate with custom truststore");

        // Store original settings
        SSLSocketFactory originalSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        SSLContext originalSSLContext = SSLContext.getDefault();

        addTrustedRoots(additionalCAs);

        try {
            List<X509Certificate> certs = fetchCertificates(hostname, port);
            logger.info("Certificates fetched successfully");
            return certs;
        } catch (Exception e) {
            logger.error("Failed to fetch certificates!", e);
            throw e;
        } finally {
            // Always restore — whether success or failure
            SSLContext.setDefault(originalSSLContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(originalSocketFactory);
            HttpsURLConnection.setDefaultHostnameVerifier(originalHostnameVerifier);
            logger.info("SSL context restored to original");
        }
    }

    /**
     * Fetches certificate WITHOUT validation (for self-signed/expired certs).
     * IMPORTANT: This bypasses security checks - use only when appropriate!
     */
    private static List<X509Certificate> fetchCertificateWithoutChainValidation(String hostname, int port) throws NoSuchAlgorithmException, KeyManagementException {
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
            enableCertificateValidation(originalSocketFactory, originalHostnameVerifier);
            logger.info("Certificate validation re-enabled");
        }
    }

    private static void logCertificateDetails(X509Certificate x509, String certType) {
        logger.info("certificate details: Subject: [{}], Issuer: [{}], Validity: [{}] to [{}], Serial: [{}], type: [{}], EKU: {}", x509.getSubjectDN(), x509.getIssuerDN(), x509.getNotBefore(), x509.getNotAfter(), x509.getSerialNumber(), certType, getExtendedKeyUsage(x509));
    }

    private static List<X509Certificate> orderChain(List<X509Certificate> certs) {
        Map<Principal, X509Certificate> bySubject = new HashMap<>();
        Set<Principal> issuers = new HashSet<>();

        for (X509Certificate cert : certs) {
            bySubject.put(cert.getSubjectX500Principal(), cert);
            issuers.add(cert.getIssuerX500Principal());
        }

        // Single cert case — self-signed, treat it as the leaf
        if (certs.size() == 1) {
            return new ArrayList<>(certs);
        }

        X509Certificate leaf = certs.stream()
                .filter(c -> !c.getSubjectX500Principal().equals(c.getIssuerX500Principal()))
                .filter(c -> !issuers.contains(c.getSubjectX500Principal()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find leaf certificate"));

        List<X509Certificate> ordered = new ArrayList<>();
        X509Certificate current = leaf;
        Set<Principal> visited = new HashSet<>();

        while (current != null && visited.add(current.getSubjectX500Principal())) {
            ordered.add(current);
            // Stop if self-signed root — compare principals, not references
            if (current.getSubjectX500Principal().equals(current.getIssuerX500Principal())) {
                break;
            }
            current = bySubject.get(current.getIssuerX500Principal());
        }

        return ordered;
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

    private static void addTrustedRoots(X509Certificate... additionalCAs) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
        // Load the default system truststore (JDK cacerts)
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null);

        KeyStore customTrustStore = KeyStore.getInstance("PKCS12");
        customTrustStore.load(null, null);
        for (int i = 0; i < additionalCAs.length; i++) {
            customTrustStore.setCertificateEntry("custom-root-" + i, additionalCAs[i]);
        }

        X509Certificate x509Certificate = (X509Certificate)customTrustStore.getCertificate("custom-root-"+0);
        x509Certificate.checkValidity();
        System.out.println("x509Certificate.getIssuerX500Principal().getName() --> " +x509Certificate.getIssuerX500Principal().getName());
        X509Certificate x509Certificate1 = (X509Certificate)customTrustStore.getCertificate("custom-root-"+1);
        x509Certificate1.checkValidity();
        System.out.println("x509Certificate.getIssuerX500Principal().getName() --> " +x509Certificate1.getIssuerX500Principal().getName());

        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(customTrustStore);

        X509TrustManager defaultTm = (X509TrustManager) defaultTmf.getTrustManagers()[0];
        X509TrustManager customTm  = (X509TrustManager) customTmf.getTrustManagers()[0];

        X509TrustManager combinedTm = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                try {
                    defaultTm.checkClientTrusted(chain, authType);
                } catch (CertificateException e) {
                    customTm.checkClientTrusted(chain, authType);
                }
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                try {
                    defaultTm.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    customTm.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] defaultIssuers = defaultTm.getAcceptedIssuers();
                X509Certificate[] customIssuers  = customTm.getAcceptedIssuers();
                X509Certificate[] combined = new X509Certificate[
                        defaultIssuers.length + customIssuers.length];
                System.arraycopy(defaultIssuers, 0, combined, 0, defaultIssuers.length);
                System.arraycopy(customIssuers, 0, combined, defaultIssuers.length, customIssuers.length);
                return combined;
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{ combinedTm }, new SecureRandom());
        SSLContext.setDefault(sc);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Diagnostic — print what customTm sees
        System.out.println("Custom TM accepted issuers count: " + customTm.getAcceptedIssuers().length);
        for (X509Certificate issuer : customTm.getAcceptedIssuers()) {
            System.out.println("  Custom trusted: " + issuer.getSubjectX500Principal().getName());
        }
    }

    private static void enableCertificateValidation(
            SSLSocketFactory originalSocketFactory,
            HostnameVerifier originalHostnameVerifier) throws NoSuchAlgorithmException, KeyManagementException {

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

    private static String getExtendedKeyUsage(X509Certificate cert) {
        StringBuilder result = new StringBuilder("Extended Key Usage:");
        List<String> ekuOIDs;
        try {
            ekuOIDs = cert.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            // this would not occur in 99.99% scenarios, but just in case...
            logger.error("Failed to parse certificate EKU", e);
            throw new RuntimeException(e);
        }

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
}