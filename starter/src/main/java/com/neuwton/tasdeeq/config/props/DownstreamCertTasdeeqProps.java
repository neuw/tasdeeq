package com.neuwton.tasdeeq.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "neuwton.tasdeeq.cert")
public class DownstreamCertTasdeeqProps {

    private boolean enabled = true;
    private Map<String, DomainProps> domains = Map.of();
    private boolean strictChainValidation = true;

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

    public boolean isStrictChainValidation() {
        return strictChainValidation;
    }

    public void setStrictChainValidation(boolean strictChainValidation) {
        this.strictChainValidation = strictChainValidation;
    }

    public static class DomainProps {

        private String host;
        private int port = 443;
        private boolean validateChain = true;

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

        public void setValidateChain(boolean validateChain) {
            this.validateChain = validateChain;
        }
    }
}
