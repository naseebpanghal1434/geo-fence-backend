package com.tse.core_application.service.attendance;

import com.tse.core_application.constants.attendance.EventKind;
import com.tse.core_application.constants.attendance.ExceptionCode;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.entity.attendance.AttendanceEvent;
import com.tse.core_application.entity.fence.GeoFence;
import com.tse.core_application.entity.punch.PunchRequest;
import com.tse.core_application.util.GeoMath;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 6b: Business logic for validating CHECK_IN and CHECK_OUT events.
 */
@Service
public class AcceptanceRules {

    private final HolidayProvider holidayProvider;
    private final OfficePolicyProvider officePolicyProvider;

    public AcceptanceRules(HolidayProvider holidayProvider, OfficePolicyProvider officePolicyProvider) {
        this.holidayProvider = holidayProvider;
        this.officePolicyProvider = officePolicyProvider;
    }

    /**
     * Validate a CHECK_IN, CHECK_OUT, BREAK_START, or BREAK_END event.
     *
     * @param orgId         Organization ID
     * @param accountId     Account ID
     * @param eventKind     Event kind
     * @param lat           Latitude
     * @param lon           Longitude
     * @param accuracyM     GPS accuracy in meters
     * @param policy        Attendance policy
     * @param fence         Assigned fence (can be null)
     * @param todayEvents   Events for today (ordered by timestamp)
     * @return ValidationResult with verdict, fail reason, and flags
     */
    public ValidationResult validate(
            long orgId,
            long accountId,
            EventKind eventKind,
            Double lat,
            Double lon,
            Double accuracyM,
            AttendancePolicy policy,
            GeoFence fence,
            List<AttendanceEvent> todayEvents
    ) {
        Map<String, Object> flags = new HashMap<>();
        boolean success;
        String verdict;
        String failReason = null;

        // Handle BREAK events separately
        if (eventKind == EventKind.BREAK_START || eventKind == EventKind.BREAK_END) {
            return validateBreak(eventKind, todayEvents, flags);
        }

        // 1. Check accuracy gate
        if (accuracyM != null && accuracyM > policy.getAccuracyGateM()) {
            flags.put("low_accuracy", true);
            if (policy.getIntegrityPosture() == AttendancePolicy.IntegrityPosture.BLOCK) {
                return new ValidationResult(false, "FAIL", ExceptionCode.LOW_ACCURACY.name(), flags);
            }
        }

        // 2. Check geofence if fence is provided
        boolean underRange = false;
        if (fence != null && lat != null && lon != null) {
            double distance = GeoMath.distanceMeters(lat, lon, fence.getCenterLat(), fence.getCenterLng());
            underRange = distance <= policy.getFenceRadiusM();

            if (!underRange) {
                flags.put("out_of_fence", true);
                if (policy.getOutsideFencePolicy() == AttendancePolicy.OutsideFencePolicy.BLOCK) {
                    return new ValidationResult(false, "FAIL", ExceptionCode.OUTSIDE_FENCE.name(), flags);
                }
            }
        }

        // 3. Check cooldown
        OffsetDateTime lastEventTime = getLastEventTime(todayEvents);
        if (lastEventTime != null) {
            long secondsSinceLastEvent = java.time.Duration.between(lastEventTime, OffsetDateTime.now()).getSeconds();
            if (secondsSinceLastEvent < policy.getCooldownSeconds()) {
                return new ValidationResult(false, "FAIL", ExceptionCode.GRACE_EXPIRED.name(), flags);
            }
        }

        // 4. Check max punches per day
        long successfulPunchesToday = countSuccessfulPunches(todayEvents);
        if (successfulPunchesToday >= policy.getMaxSuccessfulPunchesPerDay()) {
            return new ValidationResult(false, "FAIL", ExceptionCode.CAP_REACHED.name(), flags);
        }

        long failedPunchesToday = countFailedPunches(todayEvents);
        if (failedPunchesToday >= policy.getMaxFailedPunchesPerDay()) {
            return new ValidationResult(false, "FAIL", ExceptionCode.CAP_REACHED.name(), flags);
        }

        // 5. Check state transitions for CHECK_IN/OUT
        if (eventKind == EventKind.CHECK_IN) {
            if (isCurrentlyCheckedIn(todayEvents)) {
                return new ValidationResult(false, "FAIL", ExceptionCode.DUP_CHECKIN.name(), flags);
            }
        } else if (eventKind == EventKind.CHECK_OUT) {
            if (!isCurrentlyCheckedIn(todayEvents)) {
                return new ValidationResult(false, "FAIL", ExceptionCode.MISSING_CHECKIN.name(), flags);
            }
        }

        // 6. Check office hours (for CHECK_IN)
        if (eventKind == EventKind.CHECK_IN) {
            OffsetDateTime now = OffsetDateTime.now();
            String tz = officePolicyProvider.getOperationalTimezone(orgId);
            LocalTime currentTime = now.atZoneSameInstant(ZoneId.of(tz)).toLocalTime();
            LocalTime officeStart = officePolicyProvider.getOfficeStartTime(orgId);
            LocalTime officeEnd = officePolicyProvider.getOfficeEndTime(orgId);

            // Calculate allowed window
            LocalTime earliestCheckin = officeStart.minusMinutes(policy.getAllowCheckinBeforeStartMin());
            LocalTime latestCheckin = officeStart.plusMinutes(policy.getLateCheckinAfterStartMin());

            if (currentTime.isBefore(earliestCheckin)) {
                flags.put("too_early", true);
            } else if (currentTime.isAfter(latestCheckin)) {
                flags.put("late_checkin", true);
            }
        }

        // 7. Check office hours (for CHECK_OUT)
        if (eventKind == EventKind.CHECK_OUT) {
            OffsetDateTime now = OffsetDateTime.now();
            String tz = officePolicyProvider.getOperationalTimezone(orgId);
            LocalTime currentTime = now.atZoneSameInstant(ZoneId.of(tz)).toLocalTime();
            LocalTime officeEnd = officePolicyProvider.getOfficeEndTime(orgId);

            LocalTime earliestCheckout = officeEnd.minusMinutes(policy.getAllowCheckoutBeforeEndMin());
            LocalTime latestCheckout = officeEnd.plusMinutes(policy.getMaxCheckoutAfterEndMin());

            if (currentTime.isBefore(earliestCheckout)) {
                flags.put("early_checkout", true);
            } else if (currentTime.isAfter(latestCheckout)) {
                flags.put("very_late_checkout", true);
            }
        }

        // 8. Check max working hours
        if (eventKind == EventKind.CHECK_OUT) {
            AttendanceEvent firstCheckin = getFirstCheckinToday(todayEvents);
            if (firstCheckin != null) {
                long hoursWorked = java.time.Duration.between(firstCheckin.getTsUtc(), OffsetDateTime.now()).toHours();
                if (hoursWorked > policy.getMaxWorkingHoursPerDay()) {
                    flags.put("excessive_hours", true);
                }
            }
        }

        // 9. Check if holiday
        LocalDate today = LocalDate.now(ZoneId.of(officePolicyProvider.getOperationalTimezone(orgId)));
        if (holidayProvider.isHoliday(orgId, today)) {
            flags.put("holiday", true);
        }

        // 10. Determine final verdict
        if (flags.containsKey("out_of_fence") || flags.containsKey("low_accuracy") ||
            flags.containsKey("late_checkin") || flags.containsKey("early_checkout") ||
            flags.containsKey("excessive_hours") || flags.containsKey("holiday")) {
            verdict = "WARN";
            success = true;
        } else {
            verdict = "PASS";
            success = true;
        }

        return new ValidationResult(success, verdict, failReason, flags);
    }

    private OffsetDateTime getLastEventTime(List<AttendanceEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        return events.get(events.size() - 1).getTsUtc();
    }

    private long countSuccessfulPunches(List<AttendanceEvent> events) {
        if (events == null) {
            return 0;
        }
        return events.stream().filter(AttendanceEvent::getSuccess).count();
    }

    private long countFailedPunches(List<AttendanceEvent> events) {
        if (events == null) {
            return 0;
        }
        return events.stream().filter(e -> !e.getSuccess()).count();
    }

    private boolean isCurrentlyCheckedIn(List<AttendanceEvent> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        // Walk backwards to find the last CHECK_IN or CHECK_OUT
        for (int i = events.size() - 1; i >= 0; i--) {
            AttendanceEvent event = events.get(i);
            if (event.getEventKind() == EventKind.CHECK_IN && event.getSuccess()) {
                return true;
            } else if (event.getEventKind() == EventKind.CHECK_OUT && event.getSuccess()) {
                return false;
            }
        }
        return false;
    }

    private AttendanceEvent getFirstCheckinToday(List<AttendanceEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        for (AttendanceEvent event : events) {
            if (event.getEventKind() == EventKind.CHECK_IN && event.getSuccess()) {
                return event;
            }
        }
        return null;
    }

    /**
     * Validate BREAK_START or BREAK_END event.
     */
    private ValidationResult validateBreak(EventKind eventKind, List<AttendanceEvent> todayEvents, Map<String, Object> flags) {
        // 1. Check if user has checked in today
        if (!isCurrentlyCheckedIn(todayEvents)) {
            return new ValidationResult(false, "FAIL", ExceptionCode.MISSING_CHECKIN.name(), flags);
        }

        // 2. Check current break state
        boolean onBreak = isCurrentlyOnBreak(todayEvents);

        if (eventKind == EventKind.BREAK_START) {
            if (onBreak) {
                return new ValidationResult(false, "FAIL", ExceptionCode.ALREADY_ON_BREAK.name(), flags);
            }
            // BREAK_START is valid
            return new ValidationResult(true, "PASS", null, flags);

        } else if (eventKind == EventKind.BREAK_END) {
            if (!onBreak) {
                return new ValidationResult(false, "FAIL", ExceptionCode.NOT_ON_BREAK.name(), flags);
            }
            // BREAK_END is valid
            return new ValidationResult(true, "PASS", null, flags);
        }

        return new ValidationResult(false, "FAIL", "INVALID_EVENT_KIND", flags);
    }

    private boolean isCurrentlyOnBreak(List<AttendanceEvent> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        // Walk backwards to find the last BREAK_START or BREAK_END
        for (int i = events.size() - 1; i >= 0; i--) {
            AttendanceEvent event = events.get(i);
            if (!event.getSuccess()) {
                continue;
            }
            if (event.getEventKind() == EventKind.BREAK_START) {
                return true;
            } else if (event.getEventKind() == EventKind.BREAK_END) {
                return false;
            }
        }
        return false;
    }

    /**
     * Validate PUNCHED event (supervisor/manager-triggered punch).
     *
     * @param punchRequest  The punch request being fulfilled
     * @param todayEvents   Events for today
     * @return ValidationResult
     */
    public ValidationResult validatePunched(PunchRequest punchRequest, List<AttendanceEvent> todayEvents) {
        Map<String, Object> flags = new HashMap<>();

        // 1. Check if PunchRequest is valid and pending
        if (punchRequest == null || punchRequest.getState() != PunchRequest.State.PENDING) {
            return new ValidationResult(false, "FAIL", ExceptionCode.FAILED_PUNCH.name(), flags);
        }

        // 2. Check time window
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(punchRequest.getRequestedDatetime()) || now.isAfter(punchRequest.getExpiresAt())) {
            return new ValidationResult(false, "FAIL", ExceptionCode.FAILED_PUNCH.name(), flags);
        }

        // 3. Validate user state - must have checked in today
        if (!isCurrentlyCheckedIn(todayEvents)) {
            return new ValidationResult(false, "FAIL", ExceptionCode.MISSING_CHECKIN.name(), flags);
        }

        // 4. User must not be on break
        if (isCurrentlyOnBreak(todayEvents)) {
            return new ValidationResult(false, "FAIL", ExceptionCode.BEFORE_CHECKOUT.name(), flags);
        }

        // PUNCHED event is valid
        return new ValidationResult(true, "PASS", null, flags);
    }

    /**
     * Result of validation.
     */
    public static class ValidationResult {
        private final boolean success;
        private final String verdict;
        private final String failReason;
        private final Map<String, Object> flags;

        public ValidationResult(boolean success, String verdict, String failReason, Map<String, Object> flags) {
            this.success = success;
            this.verdict = verdict;
            this.failReason = failReason;
            this.flags = flags;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getVerdict() {
            return verdict;
        }

        public String getFailReason() {
            return failReason;
        }

        public Map<String, Object> getFlags() {
            return flags;
        }
    }
}
