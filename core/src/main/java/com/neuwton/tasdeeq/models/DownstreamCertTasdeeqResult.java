package com.neuwton.tasdeeq.models;

import java.security.cert.X509Certificate;
import java.util.List;

public class DownstreamCertTasdeeqResult {

    private String host;
    private int port;
    private boolean validateChain;
    private List<X509Certificate> downstreamCertChain;
    private boolean isTrusted;
    private String connectionError;

    public String getHost() { return host; }
    public DownstreamCertTasdeeqResult setHost(String host) { this.host = host; return this; }

    public int getPort() { return port; }
    public DownstreamCertTasdeeqResult setPort(int port) { this.port = port; return this; }

    public boolean isValidateChain() { return validateChain; }
    public DownstreamCertTasdeeqResult setValidateChain(boolean validateChain) { this.validateChain = validateChain; return this; }

    public List<X509Certificate> getDownstreamCertChain() { return downstreamCertChain; }
    public DownstreamCertTasdeeqResult setDownstreamCertChain(List<X509Certificate> downstreamCertChain) { this.downstreamCertChain = downstreamCertChain; return this; }

    public boolean isTrusted() { return isTrusted; }
    public DownstreamCertTasdeeqResult setTrusted(boolean trusted) { isTrusted = trusted; return this; }

    public String getConnectionError() { return connectionError; }
    public DownstreamCertTasdeeqResult setConnectionError(String connectionError) { this.connectionError = connectionError; return this; }
}
