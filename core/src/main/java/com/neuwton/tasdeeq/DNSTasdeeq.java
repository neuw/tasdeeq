package com.neuwton.tasdeeq;

import com.neuwton.tasdeeq.models.DNSTasdeeqResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.*;

public class DNSTasdeeq {

    private static final Logger logger = LoggerFactory.getLogger(DNSTasdeeq.class);
    private static final String DNS_FACTORY = "com.sun.jndi.dns.DnsContextFactory";
    private static final String DNS_URL = "dns://";

    public static List<DNSTasdeeqResult> tasdeeq(List<String> domains, String... recordTypes) {
        if (domains != null && !domains.isEmpty()) {
            List<DNSTasdeeqResult> results = new ArrayList<>();
            domains.forEach(domain -> {
                results.add(tasdeeq(domain, recordTypes));
            });
            return results;
        }
        return null;
    }

    public static DNSTasdeeqResult tasdeeq(String domain, String... recordTypes) {
        DNSTasdeeqResult result = new DNSTasdeeqResult(domain);

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, DNS_FACTORY);
        env.put(Context.PROVIDER_URL, DNS_URL);

        InitialDirContext dirContext = null;
        try {
            dirContext = new InitialDirContext(env);

            // If no specific record types provided, query common ones
            if (recordTypes.length == 0) {
                recordTypes = new String[]{"A", "AAAA", "MX", "TXT", "NS", "CNAME"};
            }

            logger.info("Querying DNS records: {} for {}", Arrays.toString(recordTypes), domain);

            for (String recordType : recordTypes) {
                try {
                    Attributes attributes = dirContext.getAttributes(domain, new String[]{recordType});
                    Attribute attribute = attributes.get(recordType);

                    if (attribute != null) {
                        List<String> records = new ArrayList<>();
                        NamingEnumeration<?> enumeration = attribute.getAll();

                        while (enumeration.hasMore()) {
                            Object value = enumeration.next();
                            records.add(value.toString());
                        }

                        enumeration.close();
                        result.addRecords(recordType, records);
                    }
                } catch (NamingException e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("DNS name not found")) {
                        // Domain doesn't exist — no need to log per record type
                        result.setConnectionError("Domain not found: " + domain);
                        break; // No point querying other record types
                    }
                    result.addError(recordType, "Failed to query: " + msg);
                }
            }

        } catch (NamingException e) {
            result.setConnectionError("Failed to initialize DNS context: " + e.getMessage());
        } finally {
            if (dirContext != null) {
                try {
                    dirContext.close();
                } catch (NamingException e) {
                    logger.error("Error closing DNS context: {}", e.getMessage());
                }
            }
        }

        logger.info("DNS query result: {}, for domain {}", result, domain);

        return result;
    }

}
