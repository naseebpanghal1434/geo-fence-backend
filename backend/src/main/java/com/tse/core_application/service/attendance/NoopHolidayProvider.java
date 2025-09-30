package com.tse.core_application.service.attendance;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Phase 6b: Stub implementation that always returns false (no holidays).
 */
@Component
public class NoopHolidayProvider implements HolidayProvider {

    @Override
    public boolean isHoliday(long orgId, LocalDate date) {
        return false;
    }
}
