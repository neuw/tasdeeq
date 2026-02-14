package com.neuwton;

import com.neuwton.tasdeeq.JVMTasdeeq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class JVMTasdeeqTests {

    @Test
    public void testJDKVersion() throws Exception {
        JVMTasdeeq.JVMTasdeeqResult result = JVMTasdeeq.tasdeeq();
        assertNotNull(result);
        assertNotNull(result.getJdkVersion());
        assertTrue(result.getClassVersion() >= 52);
        assertNotNull(result.getVendor());
        assertNotNull(result.getJreVersion());
        assertNotNull(result.getRuntimeVersion());
    }

    @Test
    public void testJDKVersionOfClass() throws Exception {
        // the SLF4J Logger class is developed in JDK8 as base.
        assertEquals(52, JVMTasdeeq.getClassVersion(Logger.class));
    }

    @ParameterizedTest
    @CsvSource({
            "70",
            "69",
            "68",
            "67",
            "66",
            "65",
            "64",
            "63",
            "62",
            "61",
            "60",
            "59",
            "58",
            "57",
            "56",
            "55",
            "54",
            "53",
            "52"
    })
    public void testKnownJDKVersion(String majorVersion) {
        assertNotEquals("UNKNOWN", JVMTasdeeq.getJdkVersion(Integer.parseInt(majorVersion)));
    }

    @Test
    public void testUnKnownJDKVersion() {
        assertEquals("UNKNOWN", JVMTasdeeq.getJdkVersion(1000));
    }

}
