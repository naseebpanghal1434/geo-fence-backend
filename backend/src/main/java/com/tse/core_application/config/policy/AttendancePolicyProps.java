package com.tse.core_application.config.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "attendance.policy")
public class AttendancePolicyProps {

    private boolean skipOrgValidation = true;

    public boolean isSkipOrgValidation() {
        return skipOrgValidation;
    }

    public void setSkipOrgValidation(boolean skipOrgValidation) {
        this.skipOrgValidation = skipOrgValidation;
    }
}
