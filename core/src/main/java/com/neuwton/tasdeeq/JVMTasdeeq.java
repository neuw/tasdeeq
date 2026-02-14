package com.neuwton.tasdeeq;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class JVMTasdeeq {

    private static final Logger logger = LoggerFactory.getLogger(JVMTasdeeq.class.getName());

    public static JVMTasdeeqResult tasdeeq() throws IOException {
        // tasdeeq the class itself
        int classVersion = getClassVersion(JVMTasdeeq.class);
        JVMTasdeeqResult result = new JVMTasdeeqResult(
                classVersion,
                getJdkVersion(classVersion),
                System.getProperty("java.version"),
                System.getProperty("java.runtime.version"),
                System.getProperty("java.vendor"));
        logger.info("JDK Tasdeeq Result: {}", result);
        return result;
    }

    public static int getClassVersion(Class<?> clazz) throws IOException {
        String className = clazz.getName().replace('.', '/') + ".class";

        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            ClassReader reader = new ClassReader(is);

            final int[] version = new int[1];

            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int versionNum, int access, String name,
                                  String signature, String superName, String[] interfaces) {
                    version[0] = versionNum & 0xFFFF; // Extract major version
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            return version[0];
        }
    }

    public static String getJdkVersion(int majorVersion) {
        switch (majorVersion) {
            case 70: return "26";
            case 69: return "25";
            case 68: return "24";
            case 67: return "23";
            case 66: return "22";
            case 65: return "21";
            case 64: return "20";
            case 63: return "19";
            case 62: return "18";
            case 61: return "17";
            case 60: return "16";
            case 59: return "15";
            case 58: return "14";
            case 57: return "13";
            case 56: return "12";
            case 55: return "11";
            case 54: return "10";
            case 53: return "9";
            case 52: return "8";
            default: return "UNKNOWN";
        }
    }

    public static class JVMTasdeeqResult {
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

}
