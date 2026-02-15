package com.neuwton.tasdeeq.models;

import java.security.cert.X509Certificate;
import java.util.List;

public class DownstreamCertTasdeeqResult {

    private List<X509Certificate> downstreamCertChain;

    public List<X509Certificate> getDownstreamCertChain() {
        return downstreamCertChain;
    }

    public void setDownstreamCertChain(List<X509Certificate> downstreamCertChain) {
        this.downstreamCertChain = downstreamCertChain;
    }

}
