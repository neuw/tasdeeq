package com.neuwton.tasdeeq.config;

import com.neuwton.tasdeeq.CertificateAuthorityTasdeeq;
import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import com.neuwton.tasdeeq.JVMTasdeeq;
import com.neuwton.tasdeeq.config.props.CertificateAuthorityTasdeeqProps;
import com.neuwton.tasdeeq.config.props.DNSTasdeeqProps;
import com.neuwton.tasdeeq.config.props.DownstreamCertTasdeeqProps;
import com.neuwton.tasdeeq.config.props.JVMTasdeeqProps;
import com.neuwton.tasdeeq.models.CertificateAuthorityTasdeeqResult;
import com.neuwton.tasdeeq.models.DNSTasdeeqResult;
import com.neuwton.tasdeeq.models.DownstreamCertTasdeeqResult;
import com.neuwton.tasdeeq.models.JVMTasdeeqResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties({
    JVMTasdeeqProps.class, DNSTasdeeqProps.class,
    CertificateAuthorityTasdeeqProps.class, DownstreamCertTasdeeqProps.class
})
public class TasdeeqAutoConfig {

    @Bean
    @ConditionalOnProperty(prefix = "com.neuwton.tasdeeq.jvm", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JVMTasdeeqResult jvmTasdeeq() throws IOException {
        return JVMTasdeeq.tasdeeq();
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.neuwton.tasdeeq.cert", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CertificateAuthorityTasdeeqResult certificateAuthorityTasdeeq() {
        return CertificateAuthorityTasdeeq.tasdeeq();
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.neuwton.tasdeeq.dns", name = "enabled", havingValue = "true")
    public List<DNSTasdeeqResult> dnsTasdeeq(DNSTasdeeqProps props) {
        if (!CollectionUtils.isEmpty(props.getDomains())) {
            return DNSTasdeeq.tasdeeq(props.getDomains(), props.getRecords().toArray(String[]::new));
        }
        return null;
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.neuwton.tasdeeq.cert", name = "enabled", havingValue = "true")
    public List<DownstreamCertTasdeeqResult> downstreamCertTasdeeq(DownstreamCertTasdeeqProps props) {
        if (!CollectionUtils.isEmpty(props.getDomains())) {
            List<DownstreamCertTasdeeqResult> results = new ArrayList<>();
            props.getDomains().forEach((k, v) -> {
                    results.add(DownstreamCertTasdeeq.tasdeeq(v.getHost(), v.getPort(), v.isValidateChain()));
            });
            return results;
        }
        return Collections.emptyList();
    }

}
