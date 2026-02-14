package com.neuwton.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.neuwton.utils.TestConstants.*;

public class MockServer {

    private static final Logger logger = LoggerFactory.getLogger(MockServer.class);

    static {
        logger.info("MockServer initialized");
    }

    public static WireMockServer initServer(String... args) {
        int port = 8443;
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration
                .options()
                .httpDisabled(true)
                .httpsPort(port);

        System.out.println(Arrays.toString(args));

        Map<String, String> params = parseArgs(args);
        if (!params.isEmpty()) {
            port = Integer.parseInt(params.getOrDefault(SERVER_PORT, "8443"));
            System.out.println("port: " + port);
            wireMockConfiguration.httpsPort(port);

            if (params.containsKey(KEYSTORE_PATH)) {
                wireMockConfiguration.keystorePath(params.get(KEYSTORE_PATH));
                wireMockConfiguration.keystorePassword(params.getOrDefault(KEYSTORE_PASSWORD, CHANGE_IT));
                wireMockConfiguration.keyManagerPassword(params.getOrDefault(KEYSTORE_PASSWORD, CHANGE_IT)); // ← ADD
                wireMockConfiguration.keystoreType("PKCS12");
            }
        }

        WireMockServer wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.start();

        logger.info("WireMock server running on http(s)://localhost:{}", port);
        return wireMockServer;
    }

    // if one wants to run it on local, for testing may be.
    public static void main(String... args) {
        WireMockServer wireMockServer = initServer(args);
        // add more stubs if needed in that case we may use wireMockServer object to add stubs
        Runtime.getRuntime().addShutdownHook(new Thread(wireMockServer::stop));
    }

    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new LinkedHashMap<>();

        for (String arg : args) {
            int idx = arg.indexOf('=');
            if (idx <= 0) {
                throw new IllegalArgumentException("Invalid argument (expected key=value): " + arg);
            }
            map.put(arg.substring(0, idx).trim(), arg.substring(idx + 1).trim());
        }

        return map;
    }

}
