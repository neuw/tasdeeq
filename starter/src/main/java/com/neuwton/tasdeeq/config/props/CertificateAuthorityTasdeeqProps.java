package com.neuwton.tasdeeq.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.neuwton.tasdeeq.utils.TasdeeqStarterConstants.NEUWTON_TASDEEQ_CA_PREFIX;

@ConfigurationProperties(prefix = NEUWTON_TASDEEQ_CA_PREFIX)
public class CertificateAuthorityTasdeeqProps {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
