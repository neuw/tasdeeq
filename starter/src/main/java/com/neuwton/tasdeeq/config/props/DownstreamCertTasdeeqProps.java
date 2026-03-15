package com.neuwton.tasdeeq.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

import static com.neuwton.tasdeeq.utils.TasdeeqStarterConstants.NEUWTON_TASDEEQ_CERT_PREFIX;

@ConfigurationProperties(prefix = NEUWTON_TASDEEQ_CERT_PREFIX)
public class DownstreamCertTasdeeqProps {

    private boolean enabled = true;
    private int cacheTtlSeconds = 3600;
    private Map<String, DomainProps> domains = Map.of();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, DomainProps> getDomains() {
        return domains;
    }

    public void setDomains(Map<String, DomainProps> domains) {
        this.domains = domains;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public DownstreamCertTasdeeqProps setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
        return this;
    }

    public static class DomainProps {

        private String host;
        private int port = 443;
        private boolean validateChain = true;
        private String base64EncodedChain;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isValidateChain() {
            return validateChain;
        }

        public boolean validateChain() {
            return isValidateChain();
        }

        public void setValidateChain(boolean validateChain) {
            this.validateChain = validateChain;
        }

        public String getBase64EncodedChain() {
            return base64EncodedChain;
        }

        public DomainProps setBase64EncodedChain(String base64EncodedChain) {
            this.base64EncodedChain = base64EncodedChain;
            return this;
        }
    }
}
