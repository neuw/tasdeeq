package com.neuwton;

import com.neuwton.tasdeeq.DNSTasdeeq;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DNSTasdeeqTests {

    @Test
    public void testDNSQuery() {
        DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.queryDNS("google.com");
        assertNotNull(result);
        assertNotNull(result.getRecords("A"));
    }

    @Test
    public void testTXTRecords() {
        DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.queryDNS("google.com", "TXT");
        assertNotNull(result);
        assertEquals(0, result.getRecords("TXT").size());
    }

    @Test
    public void testUNKNOWNRecords() {
        DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.queryDNS("google.com", "UNKNOWN");
        assertNotNull(result);
        assertEquals(0, result.getRecords("UNKNOWN").size());
    }

    @Test
    public void testARecordOnly() {
        DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.queryDNS("google.com", "A");
        assertNotNull(result);
        assertNotNull(result.getRecords("A"));
    }

    @Test
    public void testAAndNSRecordOnly() {
        DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.queryDNS("google.com", "A", "NS");
        assertNotNull(result);
        assertNotNull(result.getRecords("A"));
        assertNotNull(result.getRecords("NS"));
    }

    @Test
    public void testDNSQueryWrongWebsite() {
        String domain = "google-not-good"; // random domain
        DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.queryDNS(domain);
        assertNotNull(result);
        assertEquals("Domain not found: "+result.getDomain(), result.getConnectionError());
    }

    @Test
    public void testAAndNSRecordOnlyWrongWebsite() {
        String domain = "google-not-good"; // random domain
        DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.queryDNS(domain, "A", "NS");
        assertNotNull(result);
        assertEquals("Domain not found: "+domain, result.getConnectionError());
    }

}
