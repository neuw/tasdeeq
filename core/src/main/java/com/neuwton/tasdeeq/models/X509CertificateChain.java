package com.neuwton.tasdeeq.models;

import java.security.cert.X509Certificate;

public class X509CertificateChain {

    private String base64EncodedCertificate;
    private X509Certificate rootCACertificate;
    private X509Certificate intermediateCACertificate;
    private X509Certificate leafCertificate;

    public String getBase64EncodedCertificate() {
        return base64EncodedCertificate;
    }

    public X509CertificateChain setBase64EncodedCertificate(String base64EncodedCertificate) {
        this.base64EncodedCertificate = base64EncodedCertificate;
        return this;
    }

    public X509Certificate getRootCACertificate() {
        return rootCACertificate;
    }

    public X509CertificateChain setRootCACertificate(X509Certificate rootCACertificate) {
        this.rootCACertificate = rootCACertificate;
        return this;
    }

    public X509Certificate getIntermediateCACertificate() {
        return intermediateCACertificate;
    }

    public X509CertificateChain setIntermediateCACertificate(X509Certificate intermediateCACertificate) {
        this.intermediateCACertificate = intermediateCACertificate;
        return this;
    }

    public X509Certificate getLeafCertificate() {
        return leafCertificate;
    }

    public X509CertificateChain setLeafCertificate(X509Certificate leafCertificate) {
        this.leafCertificate = leafCertificate;
        return this;
    }
}
