package com.tse.core_application.service.attendance;

import com.tse.core_application.constants.EntityTypes;
import com.tse.core_application.constants.attendance.AttendanceStatus;
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
import com.tse.core_application.util.GeoMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for comprehensive attendance data API.
 * Provides single source for all attendance views: detailed list, grid, and drill-down.
 */
@Service
public class AttendanceDataService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceDataService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AttendanceEventRepository eventRepository;
    private final AttendanceDayRepository dayRepository;
    private final AttendancePolicyRepository policyRepository;
    private final GeoFenceRepository fenceRepository;
    private final EntityPreferenceRepository entityPreferenceRepository;
    // TODO: Add LeaveApplicationRepository when available
    // private final LeaveApplicationRepository leaveApplicationRepository;

    public AttendanceDataService(
            AttendanceEventRepository eventRepository,
            AttendanceDayRepository dayRepository,
            AttendancePolicyRepository policyRepository,
            GeoFenceRepository fenceRepository,
            EntityPreferenceRepository entityPreferenceRepository) {
        this.eventRepository = eventRepository;
        this.dayRepository = dayRepository;
        this.policyRepository = policyRepository;
        this.fenceRepository = fenceRepository;
        this.entityPreferenceRepository = entityPreferenceRepository;
    }

    /**
     * Get comprehensive attendance data for given org, date range, and account IDs.
     */
    @Transactional(readOnly = true)
    public AttendanceDataResponse getAttendanceData(AttendanceDataRequest request) {
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

        // 4. Load all events for the date range and account IDs
        Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap = loadEvents(
                request.getOrgId(), request.getAccountIds(), fromDate, toDate);

        // 5. Load all attendance days for the date range and account IDs
        Map<Long, Map<LocalDate, AttendanceDay>> daysMap = loadAttendanceDays(
                request.getOrgId(), request.getAccountIds(), fromDate, toDate);

        // 6. Load fences for location labels
        Map<Long, GeoFence> fenceMap = loadFences(request.getOrgId());

        // 7. Build response
        AttendanceDataResponse response = new AttendanceDataResponse();

        // A) Build summary section
        response.setSummary(buildSummarySection(request, fromDate, toDate, eventsMap, daysMap, policy));

        // B) Build detailed rows
        response.setDetailedRows(buildDetailedRows(request, fromDate, toDate, eventsMap, daysMap, fenceMap, policy));

        // C) Build grid rows
        response.setGridRows(buildGridRows(request, fromDate, toDate, eventsMap, daysMap, policy));

        // D) Build drill-down map
        response.setDrillDownMap(buildDrillDownMap(request, fromDate, toDate, eventsMap, daysMap, fenceMap, policy));

        return response;
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

    private Map<Long, Map<LocalDate, List<AttendanceEvent>>> loadEvents(
            Long orgId, List<Long> accountIds, LocalDate fromDate, LocalDate toDate) {
        Map<Long, Map<LocalDate, List<AttendanceEvent>>> result = new HashMap<>();

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.plusDays(1).atStartOfDay();

        for (Long accountId : accountIds) {
            List<AttendanceEvent> events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                    orgId, accountId, start, end);

            Map<LocalDate, List<AttendanceEvent>> dateMap = events.stream()
                    .collect(Collectors.groupingBy(e -> e.getTsUtc().toLocalDate()));

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
            AttendancePolicy policy) {

        Map<String, AttendanceDataResponse.DateSummary> perDateSummary = new HashMap<>();
        AttendanceDataResponse.DateSummary overallSummary = new AttendanceDataResponse.DateSummary();

        int overallPresent = 0, overallAbsent = 0, overallOnLeave = 0;
        int overallOnHoliday = 0, overallPartial = 0, overallLate = 0, overallAlerts = 0;

        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            AttendanceDataResponse.DateSummary dateSummary = computeDateSummary(
                    request, currentDate, eventsMap, daysMap, policy);
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
            AttendancePolicy policy) {

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

            String status = determineStatus(date, events, day, policy);

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

    private List<AttendanceDataResponse.DetailedRow> buildDetailedRows(
            AttendanceDataRequest request, LocalDate fromDate, LocalDate toDate,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy) {

        List<AttendanceDataResponse.DetailedRow> rows = new ArrayList<>();

        for (Long accountId : request.getAccountIds()) {
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                AttendanceDataResponse.DetailedRow row = buildDetailedRow(
                        request.getOrgId(), accountId, currentDate, eventsMap, daysMap, fenceMap, policy);

                // Only add non-weekend rows
                if (row != null) {
                    rows.add(row);
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        return rows;
    }

    private AttendanceDataResponse.DetailedRow buildDetailedRow(
            Long orgId, Long accountId, LocalDate date,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy) {

        HolidayInfo holidayInfo = getHolidayInfo(orgId, date, accountId);

        // Skip weekends - return null
        if (holidayInfo.isWeekend()) {
            return null;
        }

        List<AttendanceEvent> events = eventsMap.getOrDefault(accountId, Collections.emptyMap())
                .getOrDefault(date, Collections.emptyList());
        AttendanceDay day = daysMap.getOrDefault(accountId, Collections.emptyMap()).get(date);

        AttendanceDataResponse.DetailedRow row = new AttendanceDataResponse.DetailedRow();
        row.setAccountId(accountId);
        // TODO: Fetch display name, avatar, team name from user service
        row.setDisplayName("User " + accountId);
        row.setAvatar(null);
        row.setTeamName(null);
        row.setDate(date.format(DATE_FORMATTER));

        // Handle holiday status
        if (holidayInfo.isPublicHoliday()) {
            row.setStatus("HOLIDAY");
            row.setCheckInTime(null);
            row.setCheckOutTime(null);
            row.setTotalBreakMinutes(0);
            row.setBreaks(Collections.emptyList());
            row.setTotalHoursMinutes(0);
            row.setTotalEffortMinutes(0);
            row.setPrimaryFenceName(null);
            row.setFlags(Collections.emptyList());
            return row;
        }

        // Handle leave status
        if (holidayInfo.isOnLeave()) {
            row.setStatus("LEAVE");
            row.setCheckInTime(null);
            row.setCheckOutTime(null);
            row.setTotalBreakMinutes(0);
            row.setBreaks(Collections.emptyList());
            row.setTotalHoursMinutes(0);
            row.setTotalEffortMinutes(0);
            row.setPrimaryFenceName(null);
            // TODO: Add leave type name from entity preference
            row.setFlags(Collections.singletonList("Leave: " + (holidayInfo.getLeaveName() != null ? holidayInfo.getLeaveName() : "Approved")));
            return row;
        }

        // Find check-in and check-out events
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        AttendanceEvent checkOutEvent = findSuccessfulEvent(events, EventKind.CHECK_OUT);

        row.setCheckInTime(checkInEvent != null ? checkInEvent.getTsUtc().toLocalTime().format(TIME_FORMATTER) : null);
        row.setCheckOutTime(checkOutEvent != null ? checkOutEvent.getTsUtc().toLocalTime().format(TIME_FORMATTER) : null);

        // Breaks
        List<AttendanceDataResponse.BreakInterval> breaks = extractBreaks(events);
        int totalBreakMinutes = breaks.stream()
                .mapToInt(AttendanceDataResponse.BreakInterval::getDurationMinutes)
                .sum();
        row.setTotalBreakMinutes(totalBreakMinutes);
        row.setBreaks(breaks);

        // Totals
        if (day != null) {
            row.setTotalHoursMinutes(day.getWorkedSeconds() / 60);
            row.setTotalEffortMinutes((day.getWorkedSeconds() - day.getBreakSeconds()) / 60);
        } else {
            row.setTotalHoursMinutes(0);
            row.setTotalEffortMinutes(0);
        }

        // Location
        if (checkInEvent != null && checkInEvent.getFenceId() != null) {
            GeoFence fence = fenceMap.get(checkInEvent.getFenceId());
            row.setPrimaryFenceName(fence != null ? fence.getName() : "Unknown");
        } else {
            row.setPrimaryFenceName(null);
        }

        // Status
        String status = determineStatus(date, events, day, policy);
        row.setStatus(status);

        // Flags
        List<String> flags = extractFlags(events, checkInEvent, checkOutEvent, policy);
        row.setFlags(flags);

        return row;
    }

    private List<AttendanceDataResponse.GridRow> buildGridRows(
            AttendanceDataRequest request, LocalDate fromDate, LocalDate toDate,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            AttendancePolicy policy) {

        List<AttendanceDataResponse.GridRow> gridRows = new ArrayList<>();

        for (Long accountId : request.getAccountIds()) {
            AttendanceDataResponse.GridRow gridRow = new AttendanceDataResponse.GridRow();
            gridRow.setAccountId(accountId);
            // TODO: Fetch display name from user service
            gridRow.setDisplayName("User " + accountId);

            Map<String, AttendanceDataResponse.GridCell> dateCells = new HashMap<>();

            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                AttendanceDataResponse.GridCell cell = buildGridCell(
                        request.getOrgId(), accountId, currentDate, eventsMap, daysMap, policy);
                dateCells.put(currentDate.format(DATE_FORMATTER), cell);

                currentDate = currentDate.plusDays(1);
            }

            gridRow.setDateCells(dateCells);
            gridRows.add(gridRow);
        }

        return gridRows;
    }

    private AttendanceDataResponse.GridCell buildGridCell(
            Long orgId, Long accountId, LocalDate date,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            AttendancePolicy policy) {

        HolidayInfo holidayInfo = getHolidayInfo(orgId, date, accountId);

        // Weekend - return null status
        if (holidayInfo.isWeekend()) {
            return new AttendanceDataResponse.GridCell(null, null);
        }

        if (holidayInfo.isPublicHoliday()) {
            return new AttendanceDataResponse.GridCell("HOLIDAY", null);
        }

        if (holidayInfo.isOnLeave()) {
            return new AttendanceDataResponse.GridCell("LEAVE", null);
        }

        List<AttendanceEvent> events = eventsMap.getOrDefault(accountId, Collections.emptyMap())
                .getOrDefault(date, Collections.emptyList());
        AttendanceDay day = daysMap.getOrDefault(accountId, Collections.emptyMap()).get(date);

        String status = determineStatus(date, events, day, policy);

        // Determine badge
        String badge = null;
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        if (checkInEvent != null && isLateCheckIn(checkInEvent, policy)) {
            badge = "LATE_IN";
        } else if (checkInEvent != null && !checkInEvent.getUnderRange()) {
            badge = "OUTSIDE_FENCE";
        }

        return new AttendanceDataResponse.GridCell(status, badge);
    }

    private Map<String, AttendanceDataResponse.DrillDownData> buildDrillDownMap(
            AttendanceDataRequest request, LocalDate fromDate, LocalDate toDate,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy) {

        Map<String, AttendanceDataResponse.DrillDownData> drillDownMap = new HashMap<>();

        for (Long accountId : request.getAccountIds()) {
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                AttendanceDataResponse.DrillDownData drillDown = buildDrillDownData(
                        request.getOrgId(), accountId, currentDate, eventsMap, daysMap, fenceMap, policy);

                if (drillDown != null) {
                    String key = accountId + "_" + currentDate.format(DATE_FORMATTER);
                    drillDownMap.put(key, drillDown);
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        return drillDownMap;
    }

    private AttendanceDataResponse.DrillDownData buildDrillDownData(
            Long orgId, Long accountId, LocalDate date,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy) {

        HolidayInfo holidayInfo = getHolidayInfo(orgId, date, accountId);

        // Skip weekends
        if (holidayInfo.isWeekend()) {
            return null;
        }

        List<AttendanceEvent> events = eventsMap.getOrDefault(accountId, Collections.emptyMap())
                .getOrDefault(date, Collections.emptyList());
        AttendanceDay day = daysMap.getOrDefault(accountId, Collections.emptyMap()).get(date);

        AttendanceDataResponse.DrillDownData drillDown = new AttendanceDataResponse.DrillDownData();
        drillDown.setAccountId(accountId);
        drillDown.setDate(date.format(DATE_FORMATTER));

        // Day summary
        AttendanceDataResponse.DaySummary daySummary = new AttendanceDataResponse.DaySummary();
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        AttendanceEvent checkOutEvent = findSuccessfulEvent(events, EventKind.CHECK_OUT);

        daySummary.setCheckInTime(checkInEvent != null ? checkInEvent.getTsUtc().toLocalTime().format(TIME_FORMATTER) : null);
        daySummary.setCheckOutTime(checkOutEvent != null ? checkOutEvent.getTsUtc().toLocalTime().format(TIME_FORMATTER) : null);

        if (day != null) {
            daySummary.setTotalHoursMinutes(day.getWorkedSeconds() / 60);
            daySummary.setTotalEffortMinutes((day.getWorkedSeconds() - day.getBreakSeconds()) / 60);
            daySummary.setBreakMinutes(day.getBreakSeconds() / 60);
        } else {
            daySummary.setTotalHoursMinutes(0);
            daySummary.setTotalEffortMinutes(0);
            daySummary.setBreakMinutes(0);
        }

        // TODO: Calculate office hours from policy (e.g., 9 hours = 540 minutes)
        daySummary.setOfficeHoursMinutes(policy.getMaxWorkingHoursPerDay() * 60);

        if (holidayInfo.isPublicHoliday()) {
            daySummary.setNotes("Public Holiday");
        } else if (holidayInfo.isOnLeave()) {
            daySummary.setNotes("On Leave: " + (holidayInfo.getLeaveName() != null ? holidayInfo.getLeaveName() : "Approved"));
        } else {
            daySummary.setNotes(null);
        }

        drillDown.setDaySummary(daySummary);

        // Timeline
        List<AttendanceDataResponse.PunchEvent> timeline = buildTimeline(events, fenceMap, policy, date, checkInEvent, checkOutEvent);
        drillDown.setTimeline(timeline);

        return drillDown;
    }

    private List<AttendanceDataResponse.PunchEvent> buildTimeline(
            List<AttendanceEvent> events, Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy, LocalDate date,
            AttendanceEvent checkInEvent, AttendanceEvent checkOutEvent) {

        List<AttendanceDataResponse.PunchEvent> timeline = new ArrayList<>();

        // Add actual events
        for (AttendanceEvent event : events) {
            AttendanceDataResponse.PunchEvent punchEvent = new AttendanceDataResponse.PunchEvent();
            punchEvent.setEventId(event.getId());
            punchEvent.setType(event.getEventKind().name());
            punchEvent.setTime(event.getTsUtc().toLocalTime().format(TIME_FORMATTER));
            punchEvent.setAttemptStatus(event.getSuccess() ? "SUCCESSFUL" : "UNSUCCESSFUL");

            // Location label
            if (event.getFenceId() != null) {
                GeoFence fence = fenceMap.get(event.getFenceId());
                if (fence != null && event.getLat() != null && event.getLon() != null) {
                    double distance = GeoMath.distanceMeters(
                            event.getLat(), event.getLon(),
                            fence.getCenterLat(), fence.getCenterLng()
                    );
                    if (event.getUnderRange()) {
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
            punchEvent.setIntegrityVerdict(event.getVerdict().name());
            punchEvent.setFailReason(event.getFailReason());

            timeline.add(punchEvent);
        }

        // Add missing check-in event if needed
        if (checkInEvent == null && !events.isEmpty()) {
            AttendanceDataResponse.PunchEvent missingCheckIn = new AttendanceDataResponse.PunchEvent();
            missingCheckIn.setEventId(null);
            missingCheckIn.setType("MISSING_CHECK_IN");
            missingCheckIn.setTime(null);
            missingCheckIn.setAttemptStatus("MISSING");
            missingCheckIn.setLocationLabel("No check-in recorded");
            missingCheckIn.setLat(null);
            missingCheckIn.setLon(null);
            missingCheckIn.setWithinFence(null);
            missingCheckIn.setIntegrityVerdict(null);
            missingCheckIn.setFailReason("User did not check in");
            timeline.add(0, missingCheckIn);
        }

        // Add missing check-out event if needed
        if (checkInEvent != null && checkOutEvent == null) {
            // Check if auto check-out should have happened but didn't
            AttendanceDataResponse.PunchEvent missingCheckOut = new AttendanceDataResponse.PunchEvent();
            missingCheckOut.setEventId(null);
            missingCheckOut.setType("MISSING_CHECK_OUT");
            missingCheckOut.setTime(null);
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

    private List<AttendanceDataResponse.BreakInterval> extractBreaks(List<AttendanceEvent> events) {
        List<AttendanceDataResponse.BreakInterval> breaks = new ArrayList<>();

        AttendanceEvent breakStart = null;
        for (AttendanceEvent event : events) {
            if (event.getEventKind() == EventKind.BREAK_START && event.getSuccess()) {
                breakStart = event;
            } else if (event.getEventKind() == EventKind.BREAK_END && event.getSuccess() && breakStart != null) {
                AttendanceDataResponse.BreakInterval breakInterval = new AttendanceDataResponse.BreakInterval();
                breakInterval.setStartTime(breakStart.getTsUtc().toLocalTime().format(TIME_FORMATTER));
                breakInterval.setEndTime(event.getTsUtc().toLocalTime().format(TIME_FORMATTER));

                long durationMinutes = ChronoUnit.MINUTES.between(breakStart.getTsUtc(), event.getTsUtc());
                breakInterval.setDurationMinutes((int) durationMinutes);

                breaks.add(breakInterval);
                breakStart = null;
            }
        }

        // If break start exists but no end, add incomplete break
        if (breakStart != null) {
            AttendanceDataResponse.BreakInterval breakInterval = new AttendanceDataResponse.BreakInterval();
            breakInterval.setStartTime(breakStart.getTsUtc().toLocalTime().format(TIME_FORMATTER));
            breakInterval.setEndTime(null);
            breakInterval.setDurationMinutes(0);
            breaks.add(breakInterval);
        }

        return breaks;
    }

    private String determineStatus(LocalDate date, List<AttendanceEvent> events, AttendanceDay day, AttendancePolicy policy) {
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        AttendanceEvent checkOutEvent = findSuccessfulEvent(events, EventKind.CHECK_OUT);

        // Absent if no check-in
        if (checkInEvent == null) {
            return "ABSENT";
        }

        // Late if check-in is late
        if (isLateCheckIn(checkInEvent, policy)) {
            return "LATE";
        }

        // Partial if no check-out or insufficient effort
        if (checkOutEvent == null) {
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

    private boolean isLateCheckIn(AttendanceEvent checkInEvent, AttendancePolicy policy) {
        // TODO: Implement proper late check-in logic based on office start time
        // For now, assume office starts at 9:00 AM
        LocalTime officeStartTime = LocalTime.of(9, 0);
        LocalTime checkInTime = checkInEvent.getTsUtc().toLocalTime();
        LocalTime lateThreshold = officeStartTime.plusMinutes(policy.getLateCheckinAfterStartMin());

        return checkInTime.isAfter(lateThreshold);
    }

    private List<String> extractFlags(List<AttendanceEvent> events, AttendanceEvent checkInEvent, AttendanceEvent checkOutEvent, AttendancePolicy policy) {
        List<String> flags = new ArrayList<>();

        if (checkInEvent != null) {
            if (isLateCheckIn(checkInEvent, policy)) {
                flags.add("Late check-in");
            }
            if (!checkInEvent.getUnderRange()) {
                flags.add("Outside fence at check-in");
            }
            if ("WARN".equals(checkInEvent.getVerdict().name()) || "FAIL".equals(checkInEvent.getVerdict().name())) {
                flags.add("Integrity warning at check-in");
            }
        }

        if (checkOutEvent != null) {
            if (!checkOutEvent.getUnderRange()) {
                flags.add("Outside fence at check-out");
            }
            if ("WARN".equals(checkOutEvent.getVerdict().name()) || "FAIL".equals(checkOutEvent.getVerdict().name())) {
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
    }
}
