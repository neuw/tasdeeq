package com.neuwton.tasdeeq.config;

import com.neuwton.tasdeeq.CertificateAuthorityTasdeeq;
import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.DownstreamCertTasdeeq;
import com.neuwton.tasdeeq.JVMTasdeeq;
import com.neuwton.tasdeeq.config.actuators.contributors.health.DownstreamCertificateHealthContributor;
import com.neuwton.tasdeeq.config.actuators.contributors.info.CertificateAuthorityContributor;
import com.neuwton.tasdeeq.config.actuators.contributors.info.DNSTasdeeqInfoContributor;
import com.neuwton.tasdeeq.config.actuators.contributors.info.JVMTasdeeqContributor;
import com.neuwton.tasdeeq.config.props.CertificateAuthorityTasdeeqProps;
import com.neuwton.tasdeeq.config.props.DNSTasdeeqProps;
import com.neuwton.tasdeeq.config.props.DownstreamCertTasdeeqProps;
import com.neuwton.tasdeeq.config.props.JVMTasdeeqProps;
import com.neuwton.tasdeeq.exceptions.CertificateValidationException;
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

import static com.neuwton.tasdeeq.utils.TasdeeqStarterConstants.*;

@AutoConfiguration
@EnableConfigurationProperties({
    JVMTasdeeqProps.class, DNSTasdeeqProps.class,
    CertificateAuthorityTasdeeqProps.class, DownstreamCertTasdeeqProps.class
})
public class TasdeeqAutoConfig {

    @Bean
    @ConditionalOnProperty(prefix = NEUWTON_TASDEEQ_JVM_PREFIX, name = ENABLED, havingValue = "true", matchIfMissing = true)
    public JVMTasdeeqResult jvmTasdeeqResult() throws IOException {
        return JVMTasdeeq.tasdeeq();
    }

    @Bean
    @ConditionalOnBean(JVMTasdeeqResult.class)
    public JVMTasdeeqContributor jvmTasdeeqContributor(JVMTasdeeqResult result) {
        return new JVMTasdeeqContributor(result);
    }

    @Bean
    @ConditionalOnProperty(prefix = NEUWTON_TASDEEQ_CA_PREFIX, name = ENABLED, havingValue = "true", matchIfMissing = true)
    public CertificateAuthorityTasdeeqResult certificateAuthorityTasdeeqResult() {
        return CertificateAuthorityTasdeeq.tasdeeq();
    }

    @Bean
    @ConditionalOnBean(CertificateAuthorityTasdeeqResult.class)
    public CertificateAuthorityContributor certificateAuthorityContributor(CertificateAuthorityTasdeeqResult result) {
        return new CertificateAuthorityContributor(result);
    }

    @Bean
    @ConditionalOnProperty(prefix = NEUWTON_TASDEEQ_DNS_PREFIX, name = ENABLED, havingValue = "true")
    public DNSTasdeeqResults dnsTasdeeqResults(DNSTasdeeqProps props) {
        return DNSTasdeeq.tasdeeq(
                props.getDomains(),
                props.getRecords().toArray(String[]::new));
    }

    @Bean
    @ConditionalOnBean(DNSTasdeeqResults.class)
    @ConditionalOnEnabledHealthIndicator("dns-details")
    public DNSTasdeeqInfoContributor dnsTasdeeqContributor(DNSTasdeeqResults results,
                                                           DNSTasdeeqProps props,
                                                           JsonMapper jsonMapper) {
        return new DNSTasdeeqInfoContributor(results, props, jsonMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = NEUWTON_TASDEEQ_CERT_PREFIX, name = ENABLED, havingValue = "true")
    public DownstreamCertResults downstreamCertTasdeeqResults(DownstreamCertTasdeeqProps props) {
        DownstreamCertResults downstreamCertResults = new DownstreamCertResults();
        if (!CollectionUtils.isEmpty(props.getDomains())) {
            List<DownstreamCertTasdeeqResult> results = new ArrayList<>();
            props.getDomains().forEach((k, v) -> {
                try {
                    DownstreamCertTasdeeqResult result = DownstreamCertTasdeeq.tasdeeq(
                            v.getHost(), v.getPort(), v.isValidateChain());
                    result.setHost(v.getHost())
                            .setPort(v.getPort())
                            .setValidateChain(v.isValidateChain());
                    results.add(result);
                } catch (CertificateValidationException e) {
                    results.add(new DownstreamCertTasdeeqResult()
                            .setHost(v.getHost())
                            .setPort(v.getPort())
                            .setValidateChain(v.isValidateChain())
                            .setTrusted(false)
                            .setConnectionError(e.getMessage()));
                }
            });
            downstreamCertResults.setResults(results);
        } else {
            downstreamCertResults.setResults(Collections.emptyList());
        }
        return downstreamCertResults;
    }

    @Bean
    @ConditionalOnBean(DownstreamCertResults.class)
    @ConditionalOnEnabledHealthIndicator("downstream-certs")
    public DownstreamCertificateHealthContributor downstreamCertHealthContributor(DownstreamCertResults results,
                                                                                  DownstreamCertTasdeeqProps props) {
        return new DownstreamCertificateHealthContributor(results, props);
    }

}
