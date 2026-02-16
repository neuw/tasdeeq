package com.neuwton.tasdeeq.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DNSTasdeeqResult {
        private final String domain;
        private final Map<String, List<String>> records = new HashMap<>();
        private final Map<String, String> errors = new HashMap<>();
        private String connectionError = "no error";

        public DNSTasdeeqResult(String domain) {
            this.domain = domain;
        }

        public void addRecords(String recordType, List<String> recordList) {
            records.put(recordType, recordList);
        }

        public void addError(String recordType, String error) {
            errors.put(recordType, error);
        }

        public void setConnectionError(String error) {
            this.connectionError = error;
        }

        public String getDomain() {
            return domain;
        }

        public List<String> getRecords(String recordType) {
            return records.getOrDefault(recordType, new ArrayList<>());
        }

        public String getConnectionError() {
            return connectionError;
        }

        @Override
        public String toString() {
            return "DNSQueryResult{" +
                    "domain='" + domain + '\'' +
                    ", records=" + records +
                    ", errors=" + errors +
                    ", connectionError='" + connectionError + '\'' +
                    '}';
        }
    }