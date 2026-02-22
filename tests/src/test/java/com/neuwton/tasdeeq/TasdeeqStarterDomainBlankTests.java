package com.neuwton.tasdeeq;

import com.neuwton.tasdeeq.config.props.CertificateAuthorityTasdeeqProps;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "neuwton.tasdeeq.ca.enabled=true",
        "neuwton.tasdeeq.dns.enabled=true", // enabled config, but no domains
        "neuwton.tasdeeq.cert.enabled=true", // enabled config, but no domains
        "neuwton.tasdeeq.dns.domains=",
        "management.endpoints.web.exposure.include=*",
        "management.info.java.enabled=true",
        "management.endpoint.health.show-components=always",
        "management.health.defaults.enabled=true"
})
@AutoConfigureRestTestClient
public class TasdeeqStarterDomainBlankTests {

    @Autowired
    private CertificateAuthorityTasdeeqProps certificateAuthorityTasdeeqProps;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    @DirtiesContext
    void contextLoadTest() {
        assertEquals(CertificateAuthorityTasdeeqProps.class, certificateAuthorityTasdeeqProps.getClass());
        restTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
    }

}
