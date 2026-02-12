package com.neuwton.tasdeeq;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class JDKTasdeeq {

    private static final Logger logger = Logger.getLogger(JDKTasdeeq.class.getName());

    public static void tasdeeq() throws IOException {
        int version = getClassVersion(JDKTasdeeq.class);
        logger.info("Class version: " + version + ", Class JDK version: "+getJdkVersion(version)+", Java Version(java.version) is: " + System.getProperty("java.version") + ", Runtime Version(java.runtime.version) is: " + System.getProperty("java.runtime.version") + " , vendor(java.vendor) is: "+ System.getProperty("java.vendor"));
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

}
