package com.tse.core_application.service.attendance;

import com.tse.core_application.constants.attendance.AttendanceStatus;
import com.tse.core_application.constants.attendance.EventKind;
import com.tse.core_application.entity.attendance.AttendanceDay;
import com.tse.core_application.entity.attendance.AttendanceEvent;
import com.tse.core_application.repository.attendance.AttendanceDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Phase 6b: Service for computing and updating attendance_day rollups.
 */
@Service
public class DayRollupService {

    private final AttendanceDayRepository dayRepository;
    private final OfficePolicyProvider officePolicyProvider;

    public DayRollupService(AttendanceDayRepository dayRepository, OfficePolicyProvider officePolicyProvider) {
        this.dayRepository = dayRepository;
        this.officePolicyProvider = officePolicyProvider;
    }

    /**
     * Update the attendance_day rollup for a specific date.
     *
     * @param orgId     Organization ID
     * @param accountId Account ID
     * @param dateKey   Date (in operational timezone)
     * @param events    All events for this date (ordered by timestamp)
     */
    @Transactional
    public void updateDayRollup(long orgId, long accountId, LocalDate dateKey, List<AttendanceEvent> events) {
        Optional<AttendanceDay> existingOpt = dayRepository.findByOrgIdAndAccountIdAndDateKey(orgId, accountId, dateKey);

        AttendanceDay day;
        if (existingOpt.isPresent()) {
            day = existingOpt.get();
        } else {
            day = new AttendanceDay();
            day.setOrgId(orgId);
            day.setAccountId(accountId);
            day.setDateKey(dateKey);
            day.setWorkedSeconds(0);
            day.setBreakSeconds(0);
            day.setStatus(AttendanceStatus.ABSENT);
            day.setAnomalies(new HashMap<>());
        }

        // Compute rollup from events
        computeRollup(day, events);

        dayRepository.save(day);
    }

    private void computeRollup(AttendanceDay day, List<AttendanceEvent> events) {
        if (events == null || events.isEmpty()) {
            day.setStatus(AttendanceStatus.ABSENT);
            day.setFirstInUtc(null);
            day.setLastOutUtc(null);
            day.setWorkedSeconds(0);
            day.setBreakSeconds(0);
            return;
        }

        OffsetDateTime firstIn = null;
        OffsetDateTime lastOut = null;
        int workedSeconds = 0;
        int breakSeconds = 0;
        Map<String, Object> anomalies = new HashMap<>();

        // State tracking
        OffsetDateTime currentCheckinTime = null;
        OffsetDateTime currentBreakStartTime = null;

        for (AttendanceEvent event : events) {
            if (!event.getSuccess()) {
                continue; // Skip failed events
            }

            EventKind kind = event.getEventKind();

            if (kind == EventKind.CHECK_IN) {
                if (firstIn == null) {
                    firstIn = event.getTsUtc();
                }
                currentCheckinTime = event.getTsUtc();

            } else if (kind == EventKind.CHECK_OUT) {
                lastOut = event.getTsUtc();
                if (currentCheckinTime != null) {
                    long seconds = Duration.between(currentCheckinTime, event.getTsUtc()).getSeconds();
                    workedSeconds += seconds;
                    currentCheckinTime = null;
                } else {
                    anomalies.put("checkout_without_checkin", true);
                }

            } else if (kind == EventKind.BREAK_START) {
                currentBreakStartTime = event.getTsUtc();

            } else if (kind == EventKind.BREAK_END) {
                if (currentBreakStartTime != null) {
                    long seconds = Duration.between(currentBreakStartTime, event.getTsUtc()).getSeconds();
                    breakSeconds += seconds;
                    currentBreakStartTime = null;
                } else {
                    anomalies.put("break_end_without_start", true);
                }
            }
        }

        // Handle incomplete state
        if (currentCheckinTime != null) {
            // Still checked in - compute worked time until now
            long seconds = Duration.between(currentCheckinTime, OffsetDateTime.now()).getSeconds();
            workedSeconds += seconds;
            anomalies.put("still_checked_in", true);
        }

        if (currentBreakStartTime != null) {
            // Still on break
            long seconds = Duration.between(currentBreakStartTime, OffsetDateTime.now()).getSeconds();
            breakSeconds += seconds;
            anomalies.put("still_on_break", true);
        }

        day.setFirstInUtc(firstIn);
        day.setLastOutUtc(lastOut);
        day.setWorkedSeconds(workedSeconds);
        day.setBreakSeconds(breakSeconds);
        day.setAnomalies(anomalies);

        // Determine status
        if (firstIn == null) {
            day.setStatus(AttendanceStatus.ABSENT);
        } else if (anomalies.containsKey("still_checked_in") || anomalies.containsKey("still_on_break")) {
            day.setStatus(AttendanceStatus.INCOMPLETE);
        } else if (anomalies.containsKey("checkout_without_checkin") || anomalies.containsKey("break_end_without_start")) {
            day.setStatus(AttendanceStatus.FLAGGED);
        } else {
            day.setStatus(AttendanceStatus.PRESENT);
        }
    }

    /**
     * Get the date key for a given timestamp in the operational timezone.
     *
     * @param orgId Organization ID
     * @param ts    Timestamp
     * @return Date key (LocalDate in operational timezone)
     */
    public LocalDate getDateKey(long orgId, OffsetDateTime ts) {
        String tz = officePolicyProvider.getOperationalTimezone(orgId);
        return ts.atZoneSameInstant(ZoneId.of(tz)).toLocalDate();
    }
}
