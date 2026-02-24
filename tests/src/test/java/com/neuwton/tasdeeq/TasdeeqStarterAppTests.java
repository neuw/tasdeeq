package com.neuwton.tasdeeq;

import com.neuwton.tasdeeq.config.props.CertificateAuthorityTasdeeqProps;
import com.neuwton.utils.MockServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.file.Path;
import java.security.Security;
import java.util.UUID;

import static com.neuwton.utils.CertChainGeneratorUtil.generateFullChainKeyStoreJKS;
import static com.neuwton.utils.TestConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "tasdeeq.ca.enabled=true",
        "tasdeeq.dns.enabled=true",
        "tasdeeq.cert.enabled=true",
        "tasdeeq.cert.domains.localhost.host=localhost",
        "tasdeeq.cert.domains.localhost.port=18343",
        "tasdeeq.cert.domains.localhost.validate-chain=false",
        "tasdeeq.cert.domains.github.host=github.com",
        "tasdeeq.cert.domains.github.port=443",
        "tasdeeq.cert.domains.github.validate-chain=true",
        "tasdeeq.cert.cache-ttl-seconds=5",
        "tasdeeq.dns.domains=google.com,github.com",
        "tasdeeq.dns.records=A,AAAA,CNAME,TXT",
        "tasdeeq.dns.cache-ttl-seconds=5",
        "management.endpoints.web.exposure.include=*",
        "management.info.java.enabled=true",
        "management.endpoint.health.show-components=always",
        "management.health.defaults.enabled=true"
})
@AutoConfigureRestTestClient
public class TasdeeqStarterAppTests {

    @TempDir
    static Path TEMP_DIR;

    private static final String UNIQUE_ID = UUID.randomUUID().toString();

    private static final String JKS_FILE_NAME_FULL_CHAIN = "/" + UNIQUE_ID + "-full-chain.p12";

    @BeforeAll
    public static void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        generateFullChainKeyStoreJKS(TEMP_DIR+JKS_FILE_NAME_FULL_CHAIN);
        MockServer.initServer(SERVER_PORT+"="+18343,KEYSTORE_PATH+"="+TEMP_DIR+JKS_FILE_NAME_FULL_CHAIN, KEYSTORE_PASSWORD+"="+CHANGE_IT);
    }

    @Autowired
    private CertificateAuthorityTasdeeqProps certificateAuthorityTasdeeqProps;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    @DirtiesContext
    void contextLoadTest() throws InterruptedException {
        assertEquals(CertificateAuthorityTasdeeqProps.class, certificateAuthorityTasdeeqProps.getClass());
        restTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
        restTestClient.get().uri("/actuator/health")
                .exchange()
                .expectBody()
                .jsonPath("status")
                .isEqualTo("UP");
    }

}
