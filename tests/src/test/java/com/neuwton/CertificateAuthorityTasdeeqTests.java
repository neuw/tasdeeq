package com.neuwton;

import com.neuwton.tasdeeq.CertificateAuthorityTasdeeq;
import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CertificateAuthorityTasdeeqTests {

    @Test
    public void testRootsBySerialNumbers() {
        assertFalse(CertificateAuthorityTasdeeq.tasdeeq().getRootCAsBySerialNumber().isEmpty());
    }

    @Test
    public void testRootsBySubjectDNs() {
        assertFalse(CertificateAuthorityTasdeeq.tasdeeq().getRootCAsBySubjectDN().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "www.wikipedia.org",
            "google.com",
            "www.speedtest.net",
            "github.com"
    })
    public void testRootCAWithRootCAPresent(String domain) throws NoSuchAlgorithmException, KeyManagementException {
        List<X509Certificate> serverChain = DownstreamCertTasdeeq.tasdeeq(domain).getDownstreamCertChain();
        assertTrue(CertificateAuthorityTasdeeq.rootCAisTrusted(serverChain));
    }

    @ParameterizedTest
    @CsvSource({
            "stackoverflow.com"
    })
    public void testStackOverFlowRootCA(String domain) throws NoSuchAlgorithmException, KeyManagementException {
        // it presents an intermediate CA only, no root CA
        List<X509Certificate> serverChain = DownstreamCertTasdeeq.tasdeeq(domain).getDownstreamCertChain();
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
