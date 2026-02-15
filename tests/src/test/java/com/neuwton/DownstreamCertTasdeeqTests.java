package com.neuwton;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.neuwton.tasdeeq.CertificateAuthorityTasdeeq;
import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import com.neuwton.tasdeeq.exceptions.CertificateValidationException;
import com.neuwton.tasdeeq.models.DownstreamCertTasdeeqResult;
import com.neuwton.utils.MockServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import static com.neuwton.utils.CertChainGeneratorUtil.*;
import static com.neuwton.utils.TestConstants.*;
import static org.junit.jupiter.api.Assertions.*;

public class DownstreamCertTasdeeqTests {

    private static final Logger logger = LoggerFactory.getLogger(DownstreamCertTasdeeqTests.class);

    @TempDir
    static Path TEMP_DIR;

    private static final String UNIQUE_ID = UUID.randomUUID().toString();

    private static final String JKS_FILE_NAME_FULL_CHAIN = "/" + UNIQUE_ID + "-full-chain.p12";
    private static final String JKS_FILE_NAME_ROOT_SIGNED = "/" + UNIQUE_ID + "-root-signed.p12";
    private static final String JKS_FILE_NAME_SELF_SIGNED = "/" + UNIQUE_ID + "-self-signed.p12";

    @BeforeAll
    public static void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        generateFullChainKeyStoreJKS(TEMP_DIR+JKS_FILE_NAME_FULL_CHAIN);
        // Verify the file is readable by Sun BEFORE passing to WireMock
        /*KeyStore verify = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(JKS_FILE_NAME_FULL_CHAIN)) {
            verify.load(fis, CHANGE_IT.toCharArray());
        }
        Key key = verify.getKey("localhost", CHANGE_IT.toCharArray());

        logger.info("Pre-WireMock key check: {}", key != null ? "OK - " + key.getAlgorithm() : "FAILED");*/

        MockServer.initServer(KEYSTORE_PATH+"="+TEMP_DIR+JKS_FILE_NAME_FULL_CHAIN, KEYSTORE_PASSWORD+"="+CHANGE_IT);
    }

    @Test
    public void testDownstreamCert() throws NoSuchAlgorithmException, KeyManagementException {
        assertNotNull(DownstreamCertTasdeeq.tasdeeq("google.com"));
    }

    @Test
    public void testDownstreamCertByPort() throws NoSuchAlgorithmException, KeyManagementException {
        assertNotNull(DownstreamCertTasdeeq.tasdeeq("google.com", 443));
    }

    @Test
    public void testDownstreamCertsDisabledSSLValidation() throws NoSuchAlgorithmException, KeyManagementException {
        assertNotNull(DownstreamCertTasdeeq.tasdeeq("localhost", 8443, false));
    }

    @Test
    public void testDownstreamCertsDefaultSSLValidation() {
        // this also equivalent to the call DownstreamCertTasdeeq.getDownstreamCert("localhost", 8443, true);
        assertThrows(CertificateValidationException.class, () -> {
            DownstreamCertTasdeeq.tasdeeq("localhost", 8443);
        });
    }

    @Test
    public void testDownstreamCertsCustomTrustStore() throws Exception {
        assertThrows(CertificateValidationException.class, () -> {
            DownstreamCertTasdeeq.tasdeeq("localhost", 8443, true);
        });
        // load the keystore
        KeyStore keyStore = loadFullChainKeyStore();
        X509Certificate rootX509Cert = (X509Certificate) keyStore.getCertificate(ROOT_CA);
        X509Certificate intermediateX509Cert = (X509Certificate) keyStore.getCertificate(INTERMEDIATE_CA);
        System.out.println("Root subject:        " + rootX509Cert.getSubjectX500Principal().getName());
        System.out.println("Intermediate issuer: " + intermediateX509Cert.getIssuerX500Principal().getName());
        System.out.println("DN match: " + rootX509Cert.getSubjectX500Principal()
                .equals(intermediateX509Cert.getIssuerX500Principal()));
        List<X509Certificate> resolvedCerts = DownstreamCertTasdeeq.getDownstreamCertWithCustomTruststore("localhost", 8443, rootX509Cert, intermediateX509Cert);
        assertNotNull(resolvedCerts);
    }

    @Test
    public void testDownstreamDirectRootSignedCertKeystore() throws Exception {
        generateRootSignedCertKeyStoreJKS(TEMP_DIR+JKS_FILE_NAME_ROOT_SIGNED);
        int port = 18443;
        WireMockServer wireMockServer = MockServer
                .initServer(SERVER_PORT+"="+port,KEYSTORE_PATH+"="+TEMP_DIR+JKS_FILE_NAME_ROOT_SIGNED, KEYSTORE_PASSWORD+"="+CHANGE_IT);
        assertNotNull(DownstreamCertTasdeeq.tasdeeq("localhost", port, false));
        wireMockServer.stop();
    }

    @Test
    public void testDownstreamDirectSelfSignedCertKeystore() throws Exception {
        generateSelfSignedCertKeyStoreJKS(TEMP_DIR+JKS_FILE_NAME_SELF_SIGNED);
        int port = 28443;
        WireMockServer wireMockServer = MockServer
                .initServer(SERVER_PORT+"="+port,KEYSTORE_PATH+"="+TEMP_DIR+JKS_FILE_NAME_SELF_SIGNED, KEYSTORE_PASSWORD+"="+CHANGE_IT);
        DownstreamCertTasdeeqResult result = DownstreamCertTasdeeq.tasdeeq("localhost", port, false);
        assertNotNull(result.getDownstreamCertChain());
        assertFalse(result.isTrusted());
        assertFalse(CertificateAuthorityTasdeeq.rootCAisTrusted(result.getDownstreamCertChain()));
        wireMockServer.stop();
    }

    private KeyStore loadFullChainKeyStore() throws Exception {
        return loadKeyStore(TEMP_DIR+JKS_FILE_NAME_FULL_CHAIN);
    }

    private KeyStore loadKeyStore(String path) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(path)) {
            keyStore.load(fis, CHANGE_IT.toCharArray());
        }
        return keyStore;
    }

}