package com.neuwton.tasdeeq.config;

import com.neuwton.tasdeeq.CertificateAuthorityTasdeeq;
import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import com.neuwton.tasdeeq.JVMTasdeeq;
import com.neuwton.tasdeeq.config.actuators.contributors.health.DNSTasdeeqContributor;
import com.neuwton.tasdeeq.config.actuators.contributors.info.CertificateAuthorityContributor;
import com.neuwton.tasdeeq.config.actuators.contributors.info.JVMTasdeeqContributor;
import com.neuwton.tasdeeq.config.props.CertificateAuthorityTasdeeqProps;
import com.neuwton.tasdeeq.config.props.DNSTasdeeqProps;
import com.neuwton.tasdeeq.config.props.DownstreamCertTasdeeqProps;
import com.neuwton.tasdeeq.config.props.JVMTasdeeqProps;
import com.neuwton.tasdeeq.models.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.json.JsonMapper;

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
    @ConditionalOnProperty(prefix = "neuwton.tasdeeq.jvm", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JVMTasdeeqResult jvmTasdeeqResult() throws IOException {
        return JVMTasdeeq.tasdeeq();
    }

    @Bean
    @ConditionalOnBean(JVMTasdeeqResult.class)
    public JVMTasdeeqContributor jvmTasdeeqContributor(JVMTasdeeqResult result) {
        return new JVMTasdeeqContributor(result);
    }

    @Bean
    @ConditionalOnProperty(prefix = "neuwton.tasdeeq.ca", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CertificateAuthorityTasdeeqResult certificateAuthorityTasdeeqResult() {
        return CertificateAuthorityTasdeeq.tasdeeq();
    }

    @Bean
    @ConditionalOnBean(CertificateAuthorityTasdeeqResult.class)
    public CertificateAuthorityContributor certificateAuthorityContributor(CertificateAuthorityTasdeeqResult result) {
        return new CertificateAuthorityContributor(result);
    }

    @Bean
    @ConditionalOnProperty(prefix = "neuwton.tasdeeq.dns", name = "enabled", havingValue = "true")
    public DNSTasdeeqResults dnsTasdeeqResults(DNSTasdeeqProps props) {
        if (!CollectionUtils.isEmpty(props.getDomains())) {
            return DNSTasdeeq.tasdeeq(props.getDomains(), props.getRecords().toArray(String[]::new));
        }
        return null;
    }

    @Bean("dns")
    @ConditionalOnBean(DNSTasdeeqResults.class)
    @ConditionalOnEnabledHealthIndicator("dns-details")
    public DNSTasdeeqContributor dnsTasdeeqContributor(DNSTasdeeqResults results,
                                                       DNSTasdeeqProps props,
                                                       JsonMapper jsonMapper) {
        return new DNSTasdeeqContributor(results, props, jsonMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "com.neuwton.tasdeeq.cert", name = "enabled", havingValue = "true")
    public List<DownstreamCertTasdeeqResult> downstreamCertTasdeeqResults(DownstreamCertTasdeeqProps props) {
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
