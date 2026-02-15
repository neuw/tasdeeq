package com.neuwton;

import com.neuwton.tasdeeq.DNSTasdeeq;
import com.neuwton.tasdeeq.models.DNSQueryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DNSTasdeeqTests {

    @Test
    public void testDNSQuery() {
        DNSQueryResult result = DNSTasdeeq.tasdeeq("google.com");
        assertNotNull(result);
        assertNotNull(result.getRecords("A"));
    }

    @Test
    public void testTXTRecords() {
        DNSQueryResult result = DNSTasdeeq.tasdeeq("somewrong-domain-very-wrong-sub-domain.google.com", "TXT");
        assertNotNull(result);
        assertEquals(0, result.getRecords("TXT").size());
    }

    @Test
    public void testUNKNOWNRecords() {
        DNSQueryResult result = DNSTasdeeq.tasdeeq("google.com", "UNKNOWN");
        assertNotNull(result);
        assertEquals(0, result.getRecords("UNKNOWN").size());
    }

    @Test
    public void testARecordOnly() {
        DNSQueryResult result = DNSTasdeeq.tasdeeq("google.com", "A");
        assertNotNull(result);
        assertNotNull(result.getRecords("A"));
    }

    @Test
    public void testAAndNSRecordOnly() {
        DNSQueryResult result = DNSTasdeeq.tasdeeq("google.com", "A", "NS");
        assertNotNull(result);
        assertNotNull(result.getRecords("A"));
        assertNotNull(result.getRecords("NS"));
    }

    @Test
    public void testDNSQueryWrongWebsite() {
        String domain = "google-not-good"; // random domain
        DNSQueryResult result = DNSTasdeeq.tasdeeq(domain);
        assertNotNull(result);
        assertEquals("Domain not found: "+result.getDomain(), result.getConnectionError());
    }

    @Test
    public void testAAndNSRecordOnlyWrongWebsite() {
        String domain = "google-not-good"; // random domain
        DNSQueryResult result = DNSTasdeeq.tasdeeq(domain, "A", "NS");
        assertNotNull(result);
        assertEquals("Domain not found: "+domain, result.getConnectionError());
    }

}
