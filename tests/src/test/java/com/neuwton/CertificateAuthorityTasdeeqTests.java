package com.neuwton;

import com.neuwton.tasdeeq.CertificateAuthorityTasdeeq;
import org.junit.jupiter.api.Test;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class CertificateAuthorityTasdeeqTests {

    @Test
    public void testPopulateRootCAs() throws NoSuchAlgorithmException, KeyStoreException {
        CertificateAuthorityTasdeeq.populateRootCAs();
    }

}
