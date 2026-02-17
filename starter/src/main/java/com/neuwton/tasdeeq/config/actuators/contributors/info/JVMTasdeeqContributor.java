package com.neuwton.tasdeeq.config.actuators.contributors.info;

import com.neuwton.tasdeeq.models.JVMTasdeeqResult;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

public class JVMTasdeeqContributor implements InfoContributor {
    private final JVMTasdeeqResult result;

    public JVMTasdeeqContributor(JVMTasdeeqResult result) {
        this.result = result;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("jvm-details", result);
    }
}
