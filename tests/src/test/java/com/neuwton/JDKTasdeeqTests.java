package com.neuwton;

import com.neuwton.tasdeeq.JDKTasdeeq;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JDKTasdeeqTests {

    @Test
    public void testJDKVersion() throws Exception {
        JDKTasdeeq.tasdeeq();
    }

    @Test
    public void testJDKVersionOfClass() throws Exception {
        assertEquals(52, JDKTasdeeq.getClassVersion(Logger.class));
    }

}
