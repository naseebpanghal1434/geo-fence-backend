package com.tse.core_application.service.attendance;

import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Phase 6b: Stub implementation with default office hours (9 AM - 5 PM UTC).
 */
@Component
public class DefaultOfficePolicyProvider implements OfficePolicyProvider {

    @Override
    public LocalTime getOfficeStartTime(long orgId) {
        return LocalTime.of(9, 0); // 9:00 AM
    }

    @Override
    public LocalTime getOfficeEndTime(long orgId) {
        return LocalTime.of(17, 0); // 5:00 PM
    }

    @Override
    public String getOperationalTimezone(long orgId) {
        return "UTC";
    }
}
