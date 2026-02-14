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
        if (majorVersion >= 52 && majorVersion <= 70) {
            return String.valueOf(majorVersion - 44);
        }
        return "UNKNOWN";
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
