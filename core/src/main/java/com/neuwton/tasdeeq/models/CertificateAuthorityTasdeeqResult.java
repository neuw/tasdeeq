package com.neuwton.tasdeeq.models;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class CertificateAuthorityTasdeeqResult {

    private final Map<String, X509Certificate> rootCAsBySerialNumber = new HashMap<>();
    private final Map<String, X509Certificate> rootCAsBySubjectDN = new HashMap<>();

    public CertificateAuthorityTasdeeqResult(Map<String, X509Certificate> rootCAsBySerialNumber,
                                             Map<String, X509Certificate> rootCAsBySubjectDN) {
        this.rootCAsBySerialNumber.putAll(rootCAsBySerialNumber);
        this.rootCAsBySubjectDN.putAll(rootCAsBySubjectDN);
    }

    public Map<String, X509Certificate> getRootCAsBySerialNumber() {
        return rootCAsBySerialNumber;
    }

    public Map<String, X509Certificate> getRootCAsBySubjectDN() {
        return rootCAsBySubjectDN;
    }

}
