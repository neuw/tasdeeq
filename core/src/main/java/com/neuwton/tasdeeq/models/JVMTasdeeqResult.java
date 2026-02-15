package com.neuwton.tasdeeq.models;

public class JVMTasdeeqResult {
        private final int classVersion;
        private final String jdkVersion;
        private final String jreVersion;
        private final String runtimeVersion;
        private final String vendor;

        public JVMTasdeeqResult(int classVersion, String jdkVersion, String jreVersion, String runtimeVersion, String vendor) {
            this.classVersion = classVersion;
            this.jdkVersion = jdkVersion;
            this.jreVersion = jreVersion;
            this.runtimeVersion = runtimeVersion;
            this.vendor = vendor;
        }

        public int getClassVersion() {
            return classVersion;
        }

        public String getJdkVersion() {
            return jdkVersion;
        }

        public String getJreVersion() {
            return jreVersion;
        }

        public String getRuntimeVersion() {
            return runtimeVersion;
        }

        public String getVendor() {
            return vendor;
        }

        @Override
        public String toString() {
            return "JDK Tasdeeq Result {" +
                    "classVersion=" + classVersion +
                    ", jdkVersion='" + jdkVersion + '\'' +
                    ", javaVersion='" + jreVersion + '\'' +
                    ", runtimeVersion='" + runtimeVersion + '\'' +
                    ", vendor='" + vendor + '\'' +
                    '}';
        }
    }