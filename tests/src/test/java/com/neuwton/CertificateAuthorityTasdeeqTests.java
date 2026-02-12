package com.neuwton;

import com.neuwton.tasdeeq.CertificateAuthorityTasdeeq;
import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CertificateAuthorityTasdeeqTests {

    @Test
    public void testPopulateRootCAs() throws NoSuchAlgorithmException, KeyStoreException {
        CertificateAuthorityTasdeeq.populateRootCAs();
    }

    @Test
    public void testRootCAs() {
        // any JDK would have it.
        assertFalse(CertificateAuthorityTasdeeq.getRootCAsBySerialNumber().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "google.com",
            "www.speedtest.net",
            "github.com"
    })
    public void testRootCAWithRootCAPresent(String domain) {
        List<X509Certificate> serverChain = DownstreamCertTasdeeq.getDownstreamCert(domain);
        assertTrue(CertificateAuthorityTasdeeq.rootCAisTrusted(serverChain));
    }

    @ParameterizedTest
    @CsvSource({
            "stackoverflow.com"
    })
    public void testStackOverFlowRootCA(String domain) {
        // it presents an intermediate CA only, no root CA
        List<X509Certificate> serverChain = DownstreamCertTasdeeq.getDownstreamCert(domain);
        assertTrue(CertificateAuthorityTasdeeq.rootCAisTrusted(serverChain));
    }

    @Test
    public void testRootCANullChain() {
        assertFalse(CertificateAuthorityTasdeeq.rootCAisTrusted(null));
    }

    @Test
    public void testRootCAEmptyChain() {
        assertFalse(CertificateAuthorityTasdeeq.rootCAisTrusted(new ArrayList<>()));
    }


}
