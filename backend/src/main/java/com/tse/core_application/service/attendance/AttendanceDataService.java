package com.tse.core_application.service.attendance;

import com.tse.core_application.constants.EntityTypes;
import com.tse.core_application.constants.attendance.EventKind;
import com.tse.core_application.dto.attendance.AttendanceDataRequest;
import com.tse.core_application.dto.attendance.AttendanceDataResponse;
import com.tse.core_application.entity.attendance.AttendanceDay;
import com.tse.core_application.entity.attendance.AttendanceEvent;
import com.tse.core_application.entity.fence.GeoFence;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.entity.preference.EntityPreference;
import com.tse.core_application.exception.ProblemException;
import com.tse.core_application.repository.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.fence.GeoFenceRepository;
import com.tse.core_application.repository.policy.AttendancePolicyRepository;
import com.tse.core_application.repository.preference.EntityPreferenceRepository;
import com.tse.core_application.util.DateTimeUtils;
import com.tse.core_application.util.GeoMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for comprehensive attendance data API.
 * Provides single hierarchical structure organized by date, then by user.
 */
@Service
public class AttendanceDataService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceDataService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AttendanceEventRepository eventRepository;
    private final AttendanceDayRepository dayRepository;
    private final AttendancePolicyRepository policyRepository;
    private final GeoFenceRepository fenceRepository;
    private final EntityPreferenceRepository entityPreferenceRepository;
    private final OfficePolicyProvider officePolicyProvider;
    // TODO: Add LeaveApplicationRepository when available
    // private final LeaveApplicationRepository leaveApplicationRepository;

    public AttendanceDataService(
            AttendanceEventRepository eventRepository,
            AttendanceDayRepository dayRepository,
            AttendancePolicyRepository policyRepository,
            GeoFenceRepository fenceRepository,
            EntityPreferenceRepository entityPreferenceRepository,
            OfficePolicyProvider officePolicyProvider) {
        this.eventRepository = eventRepository;
        this.dayRepository = dayRepository;
        this.policyRepository = policyRepository;
        this.fenceRepository = fenceRepository;
        this.entityPreferenceRepository = entityPreferenceRepository;
        this.officePolicyProvider = officePolicyProvider;
    }

    /**
     * Get comprehensive attendance data for given org, date range, and account IDs.
     *
     * @param request Attendance data request with orgId, date range, and account IDs
     * @param userTimeZone User's timezone (e.g., "Asia/Kolkata", "America/New_York")
     * @return Attendance data response with all events in user's timezone
     */
    @Transactional(readOnly = true)
    public AttendanceDataResponse getAttendanceData(AttendanceDataRequest request, String userTimeZone) {
        // 1. Validate request
        validateRequest(request);

        // 2. Parse dates
        LocalDate fromDate = parseDate(request.getFromDate());
        LocalDate toDate = parseDate(request.getToDate());

        if (toDate.isBefore(fromDate)) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "Invalid date range",
                    "toDate must be after or equal to fromDate"
            );
        }

        // 3. Load org policy
        AttendancePolicy policy = policyRepository.findByOrgId(request.getOrgId())
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for org: " + request.getOrgId()
                ));

        // 4. Load user names in bulk (optimization)
        Map<Long, String> userNamesMap = getUserNamesMap(request.getAccountIds());

        // 5. Load all events for the date range and account IDs (timezone-aware)
        Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap = loadEvents(
                request.getOrgId(), request.getAccountIds(), fromDate, toDate, userTimeZone);

        // 6. Load all attendance days for the date range and account IDs
        Map<Long, Map<LocalDate, AttendanceDay>> daysMap = loadAttendanceDays(
                request.getOrgId(), request.getAccountIds(), fromDate, toDate);

        // 7. Load fences for location labels
        Map<Long, GeoFence> fenceMap = loadFences(request.getOrgId());

        // 8. Build response
        AttendanceDataResponse response = new AttendanceDataResponse();

        // A) Build summary section
        response.setSummary(buildSummarySection(request, fromDate, toDate, eventsMap, daysMap, policy, userTimeZone));

        // B) Build unified attendance data (sorted by date, then by accountId)
        response.setAttendanceData(buildAttendanceData(
                request, fromDate, toDate, eventsMap, daysMap, fenceMap, policy, userNamesMap, userTimeZone));

        return response;
    }

    /**
     * Bulk user name resolver to avoid N queries.
     * Returns map of accountId -> displayName.
     */
    private Map<Long, String> getUserNamesMap(List<Long> accountIds) {
        Map<Long, String> namesMap = new HashMap<>();

        // TODO: Implement bulk user name lookup from user service/repository
        // Example implementation:
        // List<User> users = userRepository.findAllById(accountIds);
        // for (User user : users) {
        //     String displayName = user.getFirstName() + " " + user.getLastName();
        //     namesMap.put(user.getAccountId(), displayName);
        // }

        // Placeholder: return "User {accountId}" for now
        for (Long accountId : accountIds) {
            namesMap.put(accountId, "User " + accountId);
        }

        return namesMap;
    }

    private void validateRequest(AttendanceDataRequest request) {
        if (request.getOrgId() == null || request.getOrgId() <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ORG_ID",
                    "Invalid orgId",
                    "orgId is required and must be positive"
            );
        }

        if (request.getAccountIds() == null || request.getAccountIds().isEmpty()) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ACCOUNT_IDS",
                    "Invalid accountIds",
                    "At least one accountId is required"
            );
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_FORMAT",
                    "Invalid date format",
                    "Date must be in format yyyy-MM-dd: " + dateStr
            );
        }
    }

    /**
     * Load events for the given date range in user's timezone.
     * Converts user's date range to server timezone for querying,
     * then groups events by user's local date (not server's date).
     *
     * @param userTimeZone User's timezone for proper date range conversion
     */
    private Map<Long, Map<LocalDate, List<AttendanceEvent>>> loadEvents(
            Long orgId, List<Long> accountIds, LocalDate fromDate, LocalDate toDate, String userTimeZone) {
        Map<Long, Map<LocalDate, List<AttendanceEvent>>> result = new HashMap<>();

        // Convert user's date range to server timezone
        // User wants events from 00:00:00 to 23:59:59 in THEIR timezone
        LocalDateTime userStartOfDay = fromDate.atStartOfDay();
        LocalDateTime serverStart = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(
                userStartOfDay, userTimeZone);

        LocalDateTime userEndOfDay = toDate.plusDays(1).atStartOfDay();
        LocalDateTime serverEnd = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(
                userEndOfDay, userTimeZone);

        for (Long accountId : accountIds) {
            List<AttendanceEvent> events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                    orgId, accountId, serverStart, serverEnd);

            // Group events by USER's local date (not server's date)
            Map<LocalDate, List<AttendanceEvent>> dateMap = events.stream()
                    .collect(Collectors.groupingBy(e -> {
                        // Convert server timestamp to user's timezone to get correct date
                        LocalDateTime userDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                                e.getTsUtc(), userTimeZone);
                        return userDateTime.toLocalDate();
                    }));

            result.put(accountId, dateMap);
        }

        return result;
    }

    private Map<Long, Map<LocalDate, AttendanceDay>> loadAttendanceDays(
            Long orgId, List<Long> accountIds, LocalDate fromDate, LocalDate toDate) {
        Map<Long, Map<LocalDate, AttendanceDay>> result = new HashMap<>();

        for (Long accountId : accountIds) {
            List<AttendanceDay> days = dayRepository.findByOrgIdAndAccountIdAndDateKeyBetween(
                    orgId, accountId, fromDate, toDate);

            Map<LocalDate, AttendanceDay> dateMap = days.stream()
                    .collect(Collectors.toMap(AttendanceDay::getDateKey, d -> d));

            result.put(accountId, dateMap);
        }

        return result;
    }

    private Map<Long, GeoFence> loadFences(Long orgId) {
        List<GeoFence> fences = fenceRepository.findByOrgId(orgId);
        return fences.stream()
                .collect(Collectors.toMap(GeoFence::getId, f -> f));
    }

    private AttendanceDataResponse.SummarySection buildSummarySection(
            AttendanceDataRequest request, LocalDate fromDate, LocalDate toDate,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            AttendancePolicy policy,
            String userTimeZone) {

        Map<String, AttendanceDataResponse.DateSummary> perDateSummary = new HashMap<>();
        AttendanceDataResponse.DateSummary overallSummary = new AttendanceDataResponse.DateSummary();

        int overallPresent = 0, overallAbsent = 0, overallOnLeave = 0;
        int overallOnHoliday = 0, overallPartial = 0, overallLate = 0, overallAlerts = 0;

        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            AttendanceDataResponse.DateSummary dateSummary = computeDateSummary(
                    request, currentDate, eventsMap, daysMap, policy, userTimeZone);
            perDateSummary.put(currentDate.format(DATE_FORMATTER), dateSummary);

            overallPresent += dateSummary.getPresent();
            overallAbsent += dateSummary.getAbsent();
            overallOnLeave += dateSummary.getOnLeave();
            overallOnHoliday += dateSummary.getOnHoliday();
            overallPartial += dateSummary.getPartiallyPresent();
            overallLate += dateSummary.getLatePresent();
            overallAlerts += dateSummary.getAlertsCount();

            currentDate = currentDate.plusDays(1);
        }

        overallSummary.setTotalEmployees(request.getAccountIds().size());
        overallSummary.setPresent(overallPresent);
        overallSummary.setAbsent(overallAbsent);
        overallSummary.setOnLeave(overallOnLeave);
        overallSummary.setOnHoliday(overallOnHoliday);
        overallSummary.setPartiallyPresent(overallPartial);
        overallSummary.setLatePresent(overallLate);
        overallSummary.setAlertsCount(overallAlerts);

        AttendanceDataResponse.SummarySection summary = new AttendanceDataResponse.SummarySection();
        summary.setPerDateSummary(perDateSummary);
        summary.setOverallSummary(overallSummary);

        return summary;
    }

    private AttendanceDataResponse.DateSummary computeDateSummary(
            AttendanceDataRequest request, LocalDate date,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            AttendancePolicy policy,
            String userTimeZone) {

        int present = 0, absent = 0, onLeave = 0, onHoliday = 0, partial = 0, late = 0, alerts = 0;

        for (Long accountId : request.getAccountIds()) {
            HolidayInfo holidayInfo = getHolidayInfo(request.getOrgId(), date, accountId);

            if (holidayInfo.isWeekend()) {
                // Weekend - skip from counts
                continue;
            }

            if (holidayInfo.isPublicHoliday()) {
                onHoliday++;
                continue;
            }

            if (holidayInfo.isOnLeave()) {
                onLeave++;
                continue;
            }

            List<AttendanceEvent> events = eventsMap.getOrDefault(accountId, Collections.emptyMap())
                    .getOrDefault(date, Collections.emptyList());
            AttendanceDay day = daysMap.getOrDefault(accountId, Collections.emptyMap()).get(date);

            String status = determineStatus(date, events, day, policy, userTimeZone, request.getOrgId());

            switch (status) {
                case "PRESENT":
                    present++;
                    break;
                case "LATE":
                    late++;
                    break;
                case "PARTIAL":
                    partial++;
                    break;
                case "ABSENT":
                    absent++;
                    break;
                case "PENDING":
                    // Today before office hours - don't count in any category yet
                    break;
                case "NOT_MARKED":
                    // Future date - don't count in any category
                    break;
            }

            // Count alerts
            long eventAlerts = events.stream()
                    .filter(e -> !e.getSuccess() || "WARN".equals(e.getVerdict().name()) || "FAIL".equals(e.getVerdict().name()))
                    .count();
            alerts += eventAlerts;
        }

        AttendanceDataResponse.DateSummary summary = new AttendanceDataResponse.DateSummary();
        summary.setTotalEmployees(request.getAccountIds().size());
        summary.setPresent(present);
        summary.setAbsent(absent);
        summary.setOnLeave(onLeave);
        summary.setOnHoliday(onHoliday);
        summary.setPartiallyPresent(partial);
        summary.setLatePresent(late);
        summary.setAlertsCount(alerts);

        return summary;
    }

    /**
     * Build unified attendance data organized by date (ascending), then by user (ascending by accountId).
     */
    private List<AttendanceDataResponse.DailyAttendanceData> buildAttendanceData(
            AttendanceDataRequest request, LocalDate fromDate, LocalDate toDate,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy,
            Map<Long, String> userNamesMap,
            String userTimeZone) {

        List<AttendanceDataResponse.DailyAttendanceData> attendanceData = new ArrayList<>();

        // Iterate through dates in ascending order
        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            AttendanceDataResponse.DailyAttendanceData dailyData = new AttendanceDataResponse.DailyAttendanceData();
            dailyData.setDate(currentDate.format(DATE_FORMATTER));

            // Check if this date is a weekend for the org
            // We can check using any accountId since weekend is org-level
            HolidayInfo dateHolidayInfo = getHolidayInfo(request.getOrgId(), currentDate,
                    request.getAccountIds().isEmpty() ? 0L : request.getAccountIds().get(0));
            dailyData.setIsWeekend(dateHolidayInfo.isWeekend());

            // Compute date summary
            AttendanceDataResponse.DateSummary dateSummary = computeDateSummary(
                    request, currentDate, eventsMap, daysMap, policy, userTimeZone);
            dailyData.setDateSummary(dateSummary);

            // Build user attendance list (sorted by accountId)
            List<AttendanceDataResponse.UserAttendanceData> userAttendanceList = new ArrayList<>();

            // Sort accountIds in ascending order
            List<Long> sortedAccountIds = new ArrayList<>(request.getAccountIds());
            Collections.sort(sortedAccountIds);

            for (Long accountId : sortedAccountIds) {
                AttendanceDataResponse.UserAttendanceData userAttendance = buildUserAttendanceData(
                        request.getOrgId(), accountId, currentDate,
                        eventsMap, daysMap, fenceMap, policy, userNamesMap, userTimeZone);

                // Only add non-weekend entries
                if (userAttendance != null) {
                    userAttendanceList.add(userAttendance);
                }
            }

            dailyData.setUserAttendance(userAttendanceList);
            attendanceData.add(dailyData);

            currentDate = currentDate.plusDays(1);
        }

        return attendanceData;
    }

    /**
     * Build complete attendance data for a single user on a specific date.
     * Shows actual work data even if day is weekend/holiday/leave.
     */
    private AttendanceDataResponse.UserAttendanceData buildUserAttendanceData(
            Long orgId, Long accountId, LocalDate date,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy,
            Map<Long, String> userNamesMap,
            String userTimeZone) {

        // STEP 1: Load events FIRST (before checking special day status)
        List<AttendanceEvent> events = eventsMap.getOrDefault(accountId, Collections.emptyMap())
                .getOrDefault(date, Collections.emptyList());
        AttendanceDay day = daysMap.getOrDefault(accountId, Collections.emptyMap()).get(date);

        // STEP 2: Get special day information
        HolidayInfo holidayInfo = getHolidayInfo(orgId, date, accountId);
        boolean hasEvents = !events.isEmpty();

        // STEP 3: If NO events on special day → return with special status
        if (!hasEvents) {
            AttendanceDataResponse.UserAttendanceData userData = new AttendanceDataResponse.UserAttendanceData();
            userData.setAccountId(accountId);
            userData.setDisplayName(userNamesMap.getOrDefault(accountId, "User " + accountId));

            // Skip weekends with no activity (don't show in list)
            if (holidayInfo.isWeekend()) {
                return null;
            }

            // Public holiday with no work
            if (holidayInfo.isPublicHoliday()) {
                userData.setStatus("HOLIDAY");
                userData.setCheckInTime(null);
                userData.setCheckOutTime(null);
                userData.setTotalHoursMinutes(0);
                userData.setTotalEffortMinutes(0);
                userData.setTotalBreakMinutes(0);
                userData.setBreaks(Collections.emptyList());
                userData.setPrimaryFenceName(null);
                userData.setFlags(Collections.emptyList());
                userData.setTimeline(Collections.emptyList());
                return userData;
            }

            // On leave with no work
            if (holidayInfo.isOnLeave()) {
                userData.setStatus("LEAVE");
                userData.setCheckInTime(null);
                userData.setCheckOutTime(null);
                userData.setTotalHoursMinutes(0);
                userData.setTotalEffortMinutes(0);
                userData.setTotalBreakMinutes(0);
                userData.setBreaks(Collections.emptyList());
                userData.setPrimaryFenceName(null);
                String leaveLabel = "On Leave: " + (holidayInfo.getLeaveName() != null ? holidayInfo.getLeaveName() : "Approved");
                userData.setFlags(Collections.singletonList(leaveLabel));
                userData.setTimeline(Collections.emptyList());
                return userData;
            }
        }

        // STEP 4: If HAS events → Process normally (regardless of special day)
        // This handles cases where user worked on weekend/holiday/leave
        AttendanceDataResponse.UserAttendanceData userData = new AttendanceDataResponse.UserAttendanceData();
        userData.setAccountId(accountId);
        userData.setDisplayName(userNamesMap.getOrDefault(accountId, "User " + accountId));

        // Find check-in and check-out events
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        AttendanceEvent checkOutEvent = findSuccessfulEvent(events, EventKind.CHECK_OUT);

        // Convert check-in and check-out times to user timezone
        if (checkInEvent != null) {
            LocalDateTime userCheckInDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    checkInEvent.getTsUtc(), userTimeZone);
            userData.setCheckInTime(userCheckInDateTime.toLocalTime().format(TIME_FORMATTER));
        } else {
            userData.setCheckInTime(null);
        }

        if (checkOutEvent != null) {
            LocalDateTime userCheckOutDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    checkOutEvent.getTsUtc(), userTimeZone);
            userData.setCheckOutTime(userCheckOutDateTime.toLocalTime().format(TIME_FORMATTER));
        } else {
            userData.setCheckOutTime(null);
        }

        // Breaks (convert to user timezone)
        List<AttendanceDataResponse.BreakInterval> breaks = extractBreaks(events, userTimeZone, orgId, date);
        int totalBreakMinutes = breaks.stream()
                .mapToInt(AttendanceDataResponse.BreakInterval::getDurationMinutes)
                .sum();
        userData.setTotalBreakMinutes(totalBreakMinutes);
        userData.setBreaks(breaks);

        // Totals
        if (day != null) {
            userData.setTotalHoursMinutes(day.getWorkedSeconds() / 60);
            userData.setTotalEffortMinutes((day.getWorkedSeconds() - day.getBreakSeconds()) / 60);
        } else {
            userData.setTotalHoursMinutes(0);
            userData.setTotalEffortMinutes(0);
        }

        // Location
        if (checkInEvent != null && checkInEvent.getFenceId() != null) {
            GeoFence fence = fenceMap.get(checkInEvent.getFenceId());
            userData.setPrimaryFenceName(fence != null ? fence.getName() : "Unknown");
        } else {
            userData.setPrimaryFenceName(null);
        }

        // Status
        String status = determineStatus(date, events, day, policy, userTimeZone, orgId);
        userData.setStatus(status);

        // Flags (with contextual information for special days)
        List<String> flags = extractFlags(events, checkInEvent, checkOutEvent, policy, holidayInfo, userTimeZone);
        userData.setFlags(flags);

        // Timeline (all punches with date+time, sorted chronologically)
        List<AttendanceDataResponse.PunchEvent> timeline = buildTimeline(
                events, fenceMap, policy, date, checkInEvent, checkOutEvent, userTimeZone, orgId);
        userData.setTimeline(timeline);

        return userData;
    }

    private List<AttendanceDataResponse.PunchEvent> buildTimeline(
            List<AttendanceEvent> events, Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy, LocalDate date,
            AttendanceEvent checkInEvent, AttendanceEvent checkOutEvent,
            String userTimeZone, Long orgId) {

        List<AttendanceDataResponse.PunchEvent> timeline = new ArrayList<>();

        // Determine if this date is today, past, or future in user's timezone
        LocalDate todayInUserTZ = LocalDate.now(ZoneId.of(userTimeZone));
        boolean isToday = date.equals(todayInUserTZ);
        boolean isFuture = date.isAfter(todayInUserTZ);

        // Get office hours for today's logic
        LocalTime officeStartTime = officePolicyProvider.getOfficeStartTime(orgId);
        LocalTime officeEndTime = officePolicyProvider.getOfficeEndTime(orgId);
        LocalTime currentTimeInUserTZ = isToday ? LocalTime.now(ZoneId.of(userTimeZone)) : null;

        // Add actual events (sorted by timestamp)
        for (AttendanceEvent event : events) {
            AttendanceDataResponse.PunchEvent punchEvent = new AttendanceDataResponse.PunchEvent();
            punchEvent.setEventId(event.getId());
            punchEvent.setType(event.getEventKind().name());

            // Convert server timestamp to user timezone
            LocalDateTime userDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    event.getTsUtc(), userTimeZone);
            punchEvent.setDateTime(userDateTime.format(DATETIME_FORMATTER)); // Full date+time in user timezone
            punchEvent.setAttemptStatus(event.getSuccess() ? "SUCCESSFUL" : "UNSUCCESSFUL");

            // Location label
            if (event.getFenceId() != null) {
                GeoFence fence = fenceMap.get(event.getFenceId());
                if (fence != null && event.getLat() != null && event.getLon() != null) {
                    double distance = GeoMath.distanceMeters(
                            event.getLat(), event.getLon(),
                            fence.getCenterLat(), fence.getCenterLng()
                    );
                    if (Boolean.TRUE.equals(event.getUnderRange())) {
                        punchEvent.setLocationLabel(fence.getName());
                    } else {
                        punchEvent.setLocationLabel(String.format("%.2f km away from %s", distance / 1000.0, fence.getName()));
                    }
                } else {
                    punchEvent.setLocationLabel("Unknown");
                }
            } else {
                punchEvent.setLocationLabel("No fence assigned");
            }

            punchEvent.setLat(event.getLat());
            punchEvent.setLon(event.getLon());
            punchEvent.setWithinFence(event.getUnderRange());
            punchEvent.setIntegrityVerdict(event.getVerdict() != null ? event.getVerdict().name() : "UNKNOWN");
            punchEvent.setFailReason(event.getFailReason());

            timeline.add(punchEvent);
        }

        // Add missing check-in event if needed
        // Logic:
        // - Future dates: Never show missing check-in
        // - Today: Only show if current time has passed office start time
        // - Past dates: Always show if missing
        boolean shouldShowMissingCheckIn = checkInEvent == null &&
                !isFuture && // Don't show for future dates
                (!isToday || (currentTimeInUserTZ != null && currentTimeInUserTZ.isAfter(officeStartTime)));

        if (shouldShowMissingCheckIn && !events.isEmpty()) {
            AttendanceDataResponse.PunchEvent missingCheckIn = new AttendanceDataResponse.PunchEvent();
            missingCheckIn.setEventId(null);
            missingCheckIn.setType("MISSING_CHECK_IN");
            missingCheckIn.setDateTime(date.format(DATE_FORMATTER) + " " + officeStartTime.format(TIME_FORMATTER));
            missingCheckIn.setAttemptStatus("MISSING");
            missingCheckIn.setLocationLabel("No check-in recorded");
            missingCheckIn.setLat(null);
            missingCheckIn.setLon(null);
            missingCheckIn.setWithinFence(null);
            missingCheckIn.setIntegrityVerdict(null);
            missingCheckIn.setFailReason("User did not check in");
            timeline.add(0, missingCheckIn);
        } else if (shouldShowMissingCheckIn && events.isEmpty()) {
            // No events at all - add missing check-in
            AttendanceDataResponse.PunchEvent missingCheckIn = new AttendanceDataResponse.PunchEvent();
            missingCheckIn.setEventId(null);
            missingCheckIn.setType("MISSING_CHECK_IN");
            missingCheckIn.setDateTime(date.format(DATE_FORMATTER) + " " + officeStartTime.format(TIME_FORMATTER));
            missingCheckIn.setAttemptStatus("MISSING");
            missingCheckIn.setLocationLabel("No check-in recorded");
            missingCheckIn.setLat(null);
            missingCheckIn.setLon(null);
            missingCheckIn.setWithinFence(null);
            missingCheckIn.setIntegrityVerdict(null);
            missingCheckIn.setFailReason("User did not check in");
            timeline.add(missingCheckIn);
        }

        // Add missing check-out event if needed
        // Logic:
        // - Future dates: Never show missing check-out
        // - Today: Only show if current time has passed office end time AND check-in exists
        // - Past dates: Always show if missing (and check-in exists)
        boolean shouldShowMissingCheckOut = checkInEvent != null && checkOutEvent == null &&
                !isFuture && // Don't show for future dates
                (!isToday || (currentTimeInUserTZ != null && currentTimeInUserTZ.isAfter(officeEndTime)));

        if (shouldShowMissingCheckOut) {
            // Check-in exists but no check-out
            AttendanceDataResponse.PunchEvent missingCheckOut = new AttendanceDataResponse.PunchEvent();
            missingCheckOut.setEventId(null);
            missingCheckOut.setType("MISSING_CHECK_OUT");
            missingCheckOut.setDateTime(date.format(DATE_FORMATTER) + " " + officeEndTime.format(TIME_FORMATTER));
            missingCheckOut.setAttemptStatus("MISSING");
            missingCheckOut.setLocationLabel("No check-out recorded");
            missingCheckOut.setLat(null);
            missingCheckOut.setLon(null);
            missingCheckOut.setWithinFence(null);
            missingCheckOut.setIntegrityVerdict(null);
            missingCheckOut.setFailReason("User did not check out (auto check-out may have failed)");
            timeline.add(missingCheckOut);
        }

        return timeline;
    }

    private AttendanceEvent findSuccessfulEvent(List<AttendanceEvent> events, EventKind kind) {
        return events.stream()
                .filter(e -> e.getEventKind() == kind && e.getSuccess())
                .findFirst()
                .orElse(null);
    }

    private List<AttendanceDataResponse.BreakInterval> extractBreaks(List<AttendanceEvent> events, String userTimeZone, Long orgId, LocalDate date) {
        List<AttendanceDataResponse.BreakInterval> breaks = new ArrayList<>();

        AttendanceEvent breakStart = null;
        for (AttendanceEvent event : events) {
            if (event.getEventKind() == EventKind.BREAK_START && event.getSuccess()) {
                breakStart = event;
            } else if (event.getEventKind() == EventKind.BREAK_END && event.getSuccess() && breakStart != null) {
                AttendanceDataResponse.BreakInterval breakInterval = new AttendanceDataResponse.BreakInterval();

                // Convert break times to user timezone
                LocalDateTime userBreakStartDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                        breakStart.getTsUtc(), userTimeZone);
                breakInterval.setStartTime(userBreakStartDateTime.toLocalTime().format(TIME_FORMATTER));

                LocalDateTime userBreakEndDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                        event.getTsUtc(), userTimeZone);
                breakInterval.setEndTime(userBreakEndDateTime.toLocalTime().format(TIME_FORMATTER));

                long durationMinutes = ChronoUnit.MINUTES.between(breakStart.getTsUtc(), event.getTsUtc());
                breakInterval.setDurationMinutes((int) durationMinutes);

                breaks.add(breakInterval);
                breakStart = null;
            }
        }

        // If break start exists but no end, assume break ended at office end time
        if (breakStart != null) {
            AttendanceDataResponse.BreakInterval breakInterval = new AttendanceDataResponse.BreakInterval();
            LocalDateTime userBreakStartDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    breakStart.getTsUtc(), userTimeZone);
            breakInterval.setStartTime(userBreakStartDateTime.toLocalTime().format(TIME_FORMATTER));

            // Assume break ended at office end time
            LocalTime officeEndTime = officePolicyProvider.getOfficeEndTime(orgId);
            breakInterval.setEndTime(officeEndTime.format(TIME_FORMATTER));

            // Calculate estimated duration from break start to office end time
            LocalDateTime officeEndDateTime = date.atTime(officeEndTime);
            LocalDateTime serverOfficeEndDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(
                    officeEndDateTime, userTimeZone);
            long estimatedDurationMinutes = ChronoUnit.MINUTES.between(breakStart.getTsUtc(), serverOfficeEndDateTime);
            breakInterval.setDurationMinutes((int) Math.max(0, estimatedDurationMinutes));

            breaks.add(breakInterval);
        }

        return breaks;
    }

    private String determineStatus(LocalDate date, List<AttendanceEvent> events, AttendanceDay day, AttendancePolicy policy, String userTimeZone, Long orgId) {
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        AttendanceEvent checkOutEvent = findSuccessfulEvent(events, EventKind.CHECK_OUT);

        // Determine if this date is today or future in user's timezone
        LocalDate todayInUserTZ = LocalDate.now(ZoneId.of(userTimeZone));
        boolean isToday = date.equals(todayInUserTZ);
        boolean isFuture = date.isAfter(todayInUserTZ);

        // Absent if no check-in
        if (checkInEvent == null) {
            // For future dates: Don't mark as absent (attendance can't be marked yet)
            if (isFuture) {
                return "NOT_MARKED"; // Future date - no attendance expected
            }

            // For today: Only mark as absent if office start time has passed
            if (isToday) {
                LocalTime currentTimeInUserTZ = LocalTime.now(ZoneId.of(userTimeZone));
                LocalTime officeStartTime = officePolicyProvider.getOfficeStartTime(orgId);

                // If current time is before office start, don't mark as absent yet
                if (currentTimeInUserTZ.isBefore(officeStartTime)) {
                    return "PENDING"; // Before office hours - status pending
                }
            }

            // For past dates or today after office start: Mark as absent
            return "ABSENT";
        }

        // Late if check-in is late
        if (isLateCheckIn(checkInEvent, policy, userTimeZone)) {
            return "LATE";
        }

        // Partial if no check-out or insufficient effort
        if (checkOutEvent == null) {
            // For today: Only mark as partial if office end time has passed
            if (isToday) {
                LocalTime currentTimeInUserTZ = LocalTime.now(ZoneId.of(userTimeZone));
                LocalTime officeEndTime = officePolicyProvider.getOfficeEndTime(orgId);

                // If current time is before office end, status is still in progress
                if (currentTimeInUserTZ.isBefore(officeEndTime)) {
                    return "PRESENT"; // During work hours - consider as present
                }
            }

            return "PARTIAL";
        }

        if (day != null) {
            int effortMinutes = (day.getWorkedSeconds() - day.getBreakSeconds()) / 60;
            int expectedMinutes = policy.getMaxWorkingHoursPerDay() * 60;
            if (effortMinutes < expectedMinutes * 0.8) { // Less than 80% of expected
                return "PARTIAL";
            }
        }

        return "PRESENT";
    }

    private boolean isLateCheckIn(AttendanceEvent checkInEvent, AttendancePolicy policy, String userTimeZone) {
        // Office start time is in user timezone (e.g., 9:00 AM in Asia/Kolkata)
        LocalTime officeStartTime = LocalTime.of(9, 0);

        // Convert check-in timestamp from server timezone to user timezone
        LocalDateTime userCheckInDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                checkInEvent.getTsUtc(), userTimeZone);
        LocalTime checkInTime = userCheckInDateTime.toLocalTime();

        LocalTime lateThreshold = officeStartTime.plusMinutes(policy.getLateCheckinAfterStartMin());

        return checkInTime.isAfter(lateThreshold);
    }

    /**
     * Extract flags for attendance including contextual (special day) and warning flags.
     * Contextual flags appear first, followed by warning flags.
     */
    private List<String> extractFlags(
            List<AttendanceEvent> events,
            AttendanceEvent checkInEvent,
            AttendanceEvent checkOutEvent,
            AttendancePolicy policy,
            HolidayInfo holidayInfo,
            String userTimeZone) {

        List<String> flags = new ArrayList<>();

        // CONTEXTUAL FLAGS FIRST (informational about special circumstances)
        if (!events.isEmpty()) {
            // Weekend work
            if (holidayInfo.isWeekend()) {
                flags.add("Weekend work");
            }

            // Public holiday work
            if (holidayInfo.isPublicHoliday()) {
                String holidayName = holidayInfo.getHolidayName();
                if (holidayName != null && !holidayName.isEmpty()) {
                    flags.add("Worked on Holiday: " + holidayName);
                } else {
                    flags.add("Worked on Holiday");
                }
            }

            // On leave but attended
            if (holidayInfo.isOnLeave()) {
                String leaveName = holidayInfo.getLeaveName();
                if (leaveName != null && !leaveName.isEmpty()) {
                    flags.add("On Leave: " + leaveName);
                } else {
                    flags.add("On Leave: Approved");
                }
            }
        }

        // WARNING FLAGS (policy violations and issues)
        if (checkInEvent != null) {
            if (isLateCheckIn(checkInEvent, policy, userTimeZone)) {
                flags.add("Late check-in");
            }
            if (!Boolean.TRUE.equals(checkInEvent.getUnderRange())) {
                flags.add("Outside fence at check-in");
            }
            if (checkInEvent.getVerdict() != null &&
                ("WARN".equals(checkInEvent.getVerdict().name()) || "FAIL".equals(checkInEvent.getVerdict().name()))) {
                flags.add("Integrity warning at check-in");
            }
        }

        if (checkOutEvent != null) {
            if (!Boolean.TRUE.equals(checkOutEvent.getUnderRange())) {
                flags.add("Outside fence at check-out");
            }
            if (checkOutEvent.getVerdict() != null &&
                ("WARN".equals(checkOutEvent.getVerdict().name()) || "FAIL".equals(checkOutEvent.getVerdict().name()))) {
                flags.add("Integrity warning at check-out");
            }
        }

        // Check for unsuccessful attempts
        long unsuccessfulAttempts = events.stream()
                .filter(e -> !e.getSuccess())
                .count();
        if (unsuccessfulAttempts > 0) {
            flags.add(unsuccessfulAttempts + " unsuccessful attempts");
        }

        return flags;
    }

    private HolidayInfo getHolidayInfo(Long orgId, LocalDate date, Long accountId) {
        HolidayInfo info = new HolidayInfo();

        // Check for org-level weekends & public holidays
        EntityPreference ep = entityPreferenceRepository
                .findByEntityTypeIdAndEntityId(EntityTypes.ORG, orgId)
                .orElse(null);

        if (ep != null) {
            // TODO: Check weekend/off-days from EntityPreference
            // For now, assume Saturday (6) and Sunday (7) are weekends
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                info.setWeekend(true);
                return info;
            }

            // TODO: Check public holidays from EntityPreference
            // For now, no public holidays
        }

        // TODO: Check personal leave from LeaveApplicationRepository
        // For now, no leave
        // Example:
        // boolean onLeave = leaveApplicationRepository.existsByAccountIdsAndDate(
        //         Collections.singletonList(accountId), date, approvedStatuses);
        // if (onLeave) {
        //     info.setOnLeave(true);
        //     info.setLeaveName(fetchLeaveName(accountId, date)); // Fetch from entity preference
        // }

        return info;
    }

    /**
     * Helper class to hold holiday information.
     */
    private static class HolidayInfo {
        private boolean isWeekend = false;
        private boolean isPublicHoliday = false;
        private boolean isOnLeave = false;
        private String leaveName = null;
        private String holidayName = null;

        public boolean isWeekend() {
            return isWeekend;
        }

        public void setWeekend(boolean weekend) {
            isWeekend = weekend;
        }

        public boolean isPublicHoliday() {
            return isPublicHoliday;
        }

        public void setPublicHoliday(boolean publicHoliday) {
            isPublicHoliday = publicHoliday;
        }

        public boolean isOnLeave() {
            return isOnLeave;
        }

        public void setOnLeave(boolean onLeave) {
            isOnLeave = onLeave;
        }

        public String getLeaveName() {
            return leaveName;
        }

        public void setLeaveName(String leaveName) {
            this.leaveName = leaveName;
        }

        public String getHolidayName() {
            return holidayName;
        }

        public void setHolidayName(String holidayName) {
            this.holidayName = holidayName;
        }
    }
}
