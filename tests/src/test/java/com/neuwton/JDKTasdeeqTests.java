package com.neuwton;

import com.neuwton.tasdeeq.JDKTasdeeq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class JDKTasdeeqTests {

    @Test
    public void testJDKVersion() throws Exception {
        JDKTasdeeq.tasdeeq();
    }

    @Test
    public void testJDKVersionOfClass() throws Exception {
        assertEquals(52, JDKTasdeeq.getClassVersion(Logger.class));
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
        assertNotEquals("UNKNOWN", JDKTasdeeq.getJdkVersion(Integer.parseInt(majorVersion)));
    }

    @Test
    public void testUnKnownJDKVersion() {
        assertEquals("UNKNOWN", JDKTasdeeq.getJdkVersion(1000));
    }

}
