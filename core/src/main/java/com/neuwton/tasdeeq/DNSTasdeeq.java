package com.neuwton.tasdeeq;

import com.neuwton.tasdeeq.models.DNSTasdeeqResult;
import com.neuwton.tasdeeq.models.DNSTasdeeqResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.*;
import java.util.concurrent.*;

public class DNSTasdeeq {

    private static final Logger logger = LoggerFactory.getLogger(DNSTasdeeq.class);
    private static final String DNS_FACTORY = "com.sun.jndi.dns.DnsContextFactory";
    private static final String DNS_URL = "dns://8.8.8.8"; // Use Google DNS for better reliability

    // Overloaded: parallel execution for multiple domains
    public static DNSTasdeeqResults tasdeeq(List<String> domains, String... recordTypes) {
        return tasdeeq(domains, 30, TimeUnit.SECONDS, recordTypes);
    }

    // Parallel with configurable timeout
    public static DNSTasdeeqResults tasdeeq(List<String> domains, long timeout, TimeUnit timeoutUnit, String... recordTypes) {
        if (domains == null || domains.isEmpty()) {
            DNSTasdeeqResults empty = new DNSTasdeeqResults();
            empty.setResults(Collections.<DNSTasdeeqResult>emptyList());
            return empty;
        }

        int poolSize = Math.min(domains.size(), Runtime.getRuntime().availableProcessors() * 2);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        try {
            List<Callable<DNSTasdeeqResult>> tasks = new ArrayList<Callable<DNSTasdeeqResult>>();
            for (final String domain : domains) {
                final String[] finalRecordTypes = recordTypes;
                tasks.add(new Callable<DNSTasdeeqResult>() {
                    public DNSTasdeeqResult call() {
                        return tasdeeq(domain, finalRecordTypes);
                    }
                });
            }

            List<Future<DNSTasdeeqResult>> futures = executor.invokeAll(tasks, timeout, timeoutUnit);

            List<DNSTasdeeqResult> results = new ArrayList<DNSTasdeeqResult>();
            int index = 0;
            for (Future<DNSTasdeeqResult> future : futures) {
                String domain = domains.get(index);
                try {
                    results.add(future.get());
                } catch (CancellationException e) {
                    logger.error("DNS query timed out for domain: {}", domain);
                    DNSTasdeeqResult timedOut = new DNSTasdeeqResult(domain);
                    timedOut.setConnectionError("Query timed out after " + timeout + " " + timeoutUnit);
                    results.add(timedOut);
                } catch (Exception e) {
                    logger.error("DNS query failed for domain: {}", domain, e);
                    DNSTasdeeqResult failed = new DNSTasdeeqResult(domain);
                    failed.setConnectionError("Unexpected error: " + e.getMessage());
                    results.add(failed);
                }
                index++;
            }

            DNSTasdeeqResults dnsTasdeeqResults = new DNSTasdeeqResults();
            dnsTasdeeqResults.setResults(results);
            return dnsTasdeeqResults;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("DNS batch query interrupted", e);
            DNSTasdeeqResults empty = new DNSTasdeeqResults();
            empty.setResults(Collections.<DNSTasdeeqResult>emptyList());
            return empty;
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("Executor did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Single domain query
    public static DNSTasdeeqResult tasdeeq(String domain, String... recordTypes) {
        DNSTasdeeqResult result = new DNSTasdeeqResult(domain);

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, DNS_FACTORY);
        env.put(Context.PROVIDER_URL, DNS_URL);
        env.put("com.sun.jndi.dns.timeout.initial", "3000");  // 3s initial timeout per query
        env.put("com.sun.jndi.dns.timeout.retries", "1");     // 1 retry = 6s max per query

        InitialDirContext dirContext = null;
        try {
            dirContext = new InitialDirContext(env);

            if (recordTypes.length == 0) {
                recordTypes = new String[]{"A", "AAAA", "MX", "TXT", "NS", "CNAME"};
            }

            logger.info("Querying DNS records: {} for {}", Arrays.toString(recordTypes), domain);

            int successfulQueries = 0;
            int totalQueries = recordTypes.length;

            for (String recordType : recordTypes) {
                try {
                    Attributes attributes = dirContext.getAttributes(domain, new String[]{recordType});
                    Attribute attribute = attributes.get(recordType);

                    if (attribute != null) {
                        List<String> records = new ArrayList<String>();
                        NamingEnumeration<?> enumeration = attribute.getAll();

                        while (enumeration.hasMore()) {
                            Object value = enumeration.next();
                            records.add(value.toString());
                        }

                        enumeration.close();
                        result.addRecords(recordType, records);
                        successfulQueries++;
                    }
                } catch (NamingException e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("DNS name not found")) {
                        result.setConnectionError("Domain not found: " + domain);
                        break; // no point querying other record types
                    }
                    // Log per-record-type failures but continue
                    result.addError(recordType, "Failed to query: " +
                            (msg != null ? msg : "DNS error"));
                    logger.debug("Failed to query {} record for {}: {}", recordType, domain, msg);
                }
            }

            // If ALL queries failed, set connection error
            if (successfulQueries == 0 && !result.getErrors().isEmpty()) {
                result.setConnectionError("All DNS queries failed - possible network issue or invalid domain");
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

        logger.info("DNS query result for {}: {}", domain, result);

        return result;
    }
}