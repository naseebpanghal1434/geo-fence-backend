package com.tse.core_application.service.attendance;

import com.tse.core_application.constants.EntityTypes;
import com.tse.core_application.constants.attendance.EventAction;
import com.tse.core_application.constants.attendance.EventKind;
import com.tse.core_application.constants.attendance.EventSource;
import com.tse.core_application.constants.attendance.IntegrityVerdict;
import com.tse.core_application.dto.attendance.PunchCreateRequest;
import com.tse.core_application.dto.attendance.PunchResponse;
import com.tse.core_application.dto.attendance.TodayAttendanceRequest;
import com.tse.core_application.dto.attendance.TodaySummaryResponse;
import com.tse.core_application.entity.assignment.FenceAssignment;
import com.tse.core_application.entity.attendance.AttendanceDay;
import com.tse.core_application.entity.attendance.AttendanceEvent;
import com.tse.core_application.entity.fence.GeoFence;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.entity.punch.PunchRequest;
import com.tse.core_application.exception.ProblemException;
import com.tse.core_application.repository.assignment.FenceAssignmentRepository;
import com.tse.core_application.repository.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.fence.GeoFenceRepository;
import com.tse.core_application.repository.policy.AttendancePolicyRepository;
import com.tse.core_application.repository.punch.PunchRequestRepository;
import com.tse.core_application.service.membership.MembershipProvider;
import com.tse.core_application.service.policy.PolicyGate;
import com.tse.core_application.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 6b: Orchestration service for attendance operations.
 */
@Service
public class AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceEventRepository eventRepository;
    private final AttendanceDayRepository dayRepository;
    private final AttendancePolicyRepository policyRepository;
    private final FenceAssignmentRepository assignmentRepository;
    private final GeoFenceRepository fenceRepository;
    private final PunchRequestRepository punchRequestRepository;
    private final MembershipProvider membershipProvider;
    private final PolicyGate policyGate;
    private final AcceptanceRules acceptanceRules;
    private final DayRollupService dayRollupService;
    private final OfficePolicyProvider officePolicyProvider;
    private final AttendanceDataService attendanceDataService;

    public AttendanceService(
            AttendanceEventRepository eventRepository,
            AttendanceDayRepository dayRepository,
            AttendancePolicyRepository policyRepository,
            FenceAssignmentRepository assignmentRepository,
            GeoFenceRepository fenceRepository,
            PunchRequestRepository punchRequestRepository,
            MembershipProvider membershipProvider,
            PolicyGate policyGate,
            AcceptanceRules acceptanceRules,
            DayRollupService dayRollupService,
            OfficePolicyProvider officePolicyProvider,
            AttendanceDataService attendanceDataService) {
        this.eventRepository = eventRepository;
        this.dayRepository = dayRepository;
        this.policyRepository = policyRepository;
        this.assignmentRepository = assignmentRepository;
        this.fenceRepository = fenceRepository;
        this.punchRequestRepository = punchRequestRepository;
        this.membershipProvider = membershipProvider;
        this.policyGate = policyGate;
        this.acceptanceRules = acceptanceRules;
        this.dayRollupService = dayRollupService;
        this.officePolicyProvider = officePolicyProvider;
        this.attendanceDataService = attendanceDataService;
    }

    /**
     * Process a punch event (CHECK_IN or CHECK_OUT).
     */
    @Transactional
    public PunchResponse processPunch(long orgId, PunchCreateRequest request, String timeZone) {
        // 1. Validate policy is active
        policyGate.assertPolicyActive(orgId);

        // 2. Validate request
        validatePunchRequest(request);

        // 3. Get attendance policy
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for org: " + orgId
                ));

        // 4. Parse event kind
        EventKind eventKind;
        try {
            eventKind = EventKind.valueOf(request.getEventKind());
        } catch (IllegalArgumentException e) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_EVENT_KIND",
                    "Invalid event kind",
                    "Event kind must be CHECK_IN or CHECK_OUT"
            );
        }

        // 5. Get nearest fence for user based on current location
        GeoFence fence = getNearestFenceForUser(orgId, request.getAccountId(), request.getLat(), request.getLon());

        // 6. Get today's events for validation
        LocalDate dateKey = dayRollupService.getDateKey(orgId, LocalDateTime.now());
        String tz = officePolicyProvider.getOperationalTimezone(orgId);
        LocalDateTime dayStart = dateKey.atStartOfDay();
        LocalDateTime dayEnd = dateKey.plusDays(1).atStartOfDay();

        List<AttendanceEvent> todayEvents = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, request.getAccountId(), dayStart, dayEnd
        );

        // 7. Check idempotency
        if (request.getIdempotencyKey() != null) {
            Optional<AttendanceEvent> existing = todayEvents.stream()
                    .filter(e -> request.getIdempotencyKey().equals(e.getIdempotencyKey()))
                    .findFirst();
            if (existing.isPresent()) {
                return mapToResponse(existing.get(), timeZone);
            }
        }

        // 8. Validate using AcceptanceRules
        AcceptanceRules.ValidationResult validation = acceptanceRules.validate(
                orgId,
                request.getAccountId(),
                eventKind,
                request.getLat(),
                request.getLon(),
                request.getAccuracyM(),
                policy,
                fence,
                todayEvents
        );

        // 9. Create AttendanceEvent
        AttendanceEvent event = new AttendanceEvent();
        event.setOrgId(orgId);
        event.setAccountId(request.getAccountId());
        event.setEventKind(eventKind);
        event.setEventSource(EventSource.GEOFENCE);
        event.setEventAction(EventAction.MANUAL);
        event.setTsUtc(LocalDateTime.now());

        if (request.getClientLocalTs() != null) {
            // Convert clientLocalTs from user timezone to server timezone
            LocalDateTime clientLocalTsUser = LocalDateTime.parse(request.getClientLocalTs());
            LocalDateTime clientLocalTsServer = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(clientLocalTsUser, timeZone);
            event.setClientLocalTs(clientLocalTsServer);
        }
        event.setClientTz(request.getClientTz());

        if (fence != null) {
            event.setFenceId(fence.getId());
        }
        event.setLat(request.getLat());
        event.setLon(request.getLon());
        event.setAccuracyM(request.getAccuracyM());

        // Determine under_range
        boolean underRange = false;
        if (fence != null && request.getLat() != null && request.getLon() != null) {
            double distance = com.tse.core_application.util.GeoMath.distanceMeters(
                    request.getLat(), request.getLon(),
                    fence.getCenterLat(), fence.getCenterLng()
            );
            underRange = distance <= policy.getFenceRadiusM();
        }
        event.setUnderRange(underRange);

        event.setSuccess(validation.isSuccess());
        event.setVerdict(IntegrityVerdict.valueOf(validation.getVerdict()));
        event.setFailReason(validation.getFailReason());
        event.setFlags(validation.getFlags());
        event.setIdempotencyKey(request.getIdempotencyKey());

        // 10. Save event
        AttendanceEvent savedEvent = eventRepository.save(event);

        // 11. Update day rollup
        List<AttendanceEvent> updatedEvents = new ArrayList<>(todayEvents);
        updatedEvents.add(savedEvent);
        dayRollupService.updateDayRollup(orgId, request.getAccountId(), dateKey, updatedEvents);

        // 12. Return response
        return mapToResponse(savedEvent, timeZone);
    }

    /**
     * Process a PUNCHED event (supervisor/manager-triggered punch).
     */
    @Transactional
    public PunchResponse processPunchedEvent(long orgId, long accountId, long punchRequestId, Double lat, Double lon, Double accuracyM, String timeZone) {
        // 1. Validate policy is active
        policyGate.assertPolicyActive(orgId);

        // 2. Fetch the punch request
        PunchRequest punchRequest = punchRequestRepository.findById(punchRequestId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "PUNCH_REQUEST_NOT_FOUND",
                        "Punch request not found",
                        "No punch request found with id: " + punchRequestId
                ));

        // Validate it belongs to the correct org
        if (!punchRequest.getOrgId().equals(orgId)) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "ORG_MISMATCH",
                    "Organization mismatch",
                    "Punch request does not belong to org: " + orgId
            );
        }

        // 3. Get attendance policy
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for org: " + orgId
                ));

        // 4. Get nearest fence for user based on location
        GeoFence fence = getDefaultFenceForUser(orgId, accountId);

        // 5. Get today's events for validation
        LocalDate dateKey = dayRollupService.getDateKey(orgId, LocalDateTime.now());
        String tz = officePolicyProvider.getOperationalTimezone(orgId);
        LocalDateTime dayStart = dateKey.atStartOfDay();
        LocalDateTime dayEnd = dateKey.plusDays(1).atStartOfDay();

        List<AttendanceEvent> todayEvents = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, accountId, dayStart, dayEnd
        );

        // 6. Validate using AcceptanceRules with GPS location
        AcceptanceRules.ValidationResult validation = acceptanceRules.validatePunched(
                punchRequest,
                lat,
                lon,
                accuracyM,
                policy,
                fence,
                todayEvents
        );

        // 7. Create AttendanceEvent for PUNCHED
        AttendanceEvent event = new AttendanceEvent();
        event.setOrgId(orgId);
        event.setAccountId(accountId);
        event.setEventKind(EventKind.PUNCHED);
        event.setEventSource(EventSource.SUPERVISOR);
        event.setEventAction(EventAction.AUTO);
        event.setTsUtc(LocalDateTime.now());
        event.setPunchRequestId(punchRequestId);
        event.setRequesterAccountId(punchRequest.getRequesterAccountId());

        // Set GPS location data
        if (fence != null) {
            event.setFenceId(fence.getId());
        }
        event.setLat(lat);
        event.setLon(lon);
        event.setAccuracyM(accuracyM);

        // Determine under_range
        boolean underRange = false;
        if (fence != null && lat != null && lon != null) {
            double distance = com.tse.core_application.util.GeoMath.distanceMeters(
                    lat, lon,
                    fence.getCenterLat(), fence.getCenterLng()
            );
            underRange = distance <= policy.getFenceRadiusM();
        }
        event.setUnderRange(underRange);

        event.setSuccess(validation.isSuccess());
        event.setVerdict(IntegrityVerdict.valueOf(validation.getVerdict()));
        event.setFailReason(validation.getFailReason());
        event.setFlags(validation.getFlags());

        // 8. If validation succeeded, mark punch request as FULFILLED
        if (validation.isSuccess()) {
            punchRequest.setState(PunchRequest.State.FULFILLED);
            punchRequestRepository.save(punchRequest);
        }

        // 9. Save event
        AttendanceEvent savedEvent = eventRepository.save(event);

        // 10. Update day rollup if successful
        if (validation.isSuccess()) {
            List<AttendanceEvent> updatedEvents = new ArrayList<>(todayEvents);
            updatedEvents.add(savedEvent);
            dayRollupService.updateDayRollup(orgId, accountId, dateKey, updatedEvents);
        }

        // 11. Return response
        return mapToResponse(savedEvent, timeZone);
    }

    /**
     * Get attendance summary for a specific user and date.
     * Reuses all logic from /data API for consistency.
     *
     * @param orgId Organization ID
     * @param request Request with accountId and date
     * @param timeZone User's timezone
     * @return Attendance summary with all events, missing events, and proper status
     */
    @Transactional(readOnly = true)
    public TodaySummaryResponse getTodaySummary(long orgId, TodayAttendanceRequest request, String timeZone) {
        // Use AttendanceDataService to get single user data with all the /data API logic
        com.tse.core_application.dto.attendance.AttendanceDataResponse.UserAttendanceData userData =
                attendanceDataService.getSingleUserAttendanceData(orgId, request.getAccountId(), request.getDate(), timeZone);

        // Parse date to get holiday info
        LocalDate targetDate = LocalDate.parse(request.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate todayInUserTZ = LocalDate.now(ZoneId.of(timeZone));

        // Get holiday/leave information
        boolean isWeekend = isWeekendDay(targetDate);
        boolean isHoliday = false; // TODO: Implement holiday check
        String holidayName = null;
        boolean isOnLeave = false; // TODO: Implement leave check
        String leaveName = null;

        // Build response
        TodaySummaryResponse response = new TodaySummaryResponse();
        response.setAccountId(request.getAccountId());
        response.setDisplayName(userData.getDisplayName());
        response.setDate(request.getDate());
        response.setIsWeekend(isWeekend);
        response.setIsHoliday(isHoliday);
        response.setHolidayName(holidayName);
        response.setIsOnLeave(isOnLeave);
        response.setLeaveName(leaveName);
        response.setStatus(userData.getStatus());
        response.setCheckInTime(userData.getCheckInTime());
        response.setCheckOutTime(userData.getCheckOutTime());
        response.setTotalHoursMinutes(userData.getTotalHoursMinutes());
        response.setTotalEffortMinutes(userData.getTotalEffortMinutes());
        response.setTotalBreakMinutes(userData.getTotalBreakMinutes());
        response.setPrimaryFenceName(userData.getPrimaryFenceName());
        response.setFlags(userData.getFlags());

        // Convert breaks
        List<TodaySummaryResponse.BreakInterval> breaks = userData.getBreaks() == null ? null :
                userData.getBreaks().stream()
                        .map(b -> new TodaySummaryResponse.BreakInterval(
                                b.getStartTime(),
                                b.getEndTime(),
                                b.getDurationMinutes()))
                        .collect(Collectors.toList());
        response.setBreaks(breaks);

        // Convert timeline
        List<TodaySummaryResponse.PunchEvent> timeline = userData.getTimeline() == null ? null :
                userData.getTimeline().stream()
                        .map(e -> new TodaySummaryResponse.PunchEvent(
                                e.getEventId(),
                                e.getType(),
                                e.getDateTime(),
                                e.getAttemptStatus(),
                                e.getLocationLabel(),
                                e.getLat(),
                                e.getLon(),
                                e.getWithinFence(),
                                e.getIntegrityVerdict(),
                                e.getFailReason()))
                        .collect(Collectors.toList());
        response.setTimeline(timeline);

        return response;
    }

    /**
     * Check if a date is weekend (Saturday or Sunday).
     */
    private boolean isWeekendDay(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    private void validatePunchRequest(PunchCreateRequest request) {
        if (request.getAccountId() == null || request.getAccountId() <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid accountId",
                    "accountId is required and must be positive"
            );
        }

        if (request.getEventKind() == null || request.getEventKind().isEmpty()) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid eventKind",
                    "eventKind is required"
            );
        }

        if (request.getLat() == null || request.getLon() == null) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid location",
                    "lat and lon are required"
            );
        }
    }

    /**
     * Get the nearest fence for a user based on their current location.
     * Finds all fences assigned to user's entities and returns the closest one.
     *
     * @param orgId Organization ID
     * @param accountId User account ID
     * @param userLat User's current latitude
     * @param userLon User's current longitude
     * @return The nearest GeoFence, or null if no fences are assigned
     */
    private GeoFence getNearestFenceForUser(long orgId, long accountId, double userLat, double userLon) {
        // Expand memberships
        Set<EntityRef> entities = expandMemberships(orgId, accountId);

        // Get assignments for all entities
        List<Integer> entityTypeIds = entities.stream()
                .map(e -> e.entityTypeId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> entityIds = entities.stream()
                .map(e -> e.entityId)
                .distinct()
                .collect(Collectors.toList());

        List<FenceAssignment> assignments = assignmentRepository
                .findByOrgIdAndEntityTypeIdInAndEntityIdIn(orgId, entityTypeIds, entityIds);

        if (assignments.isEmpty()) {
            return null;
        }

        // Get all unique fence IDs from assignments
        List<Long> fenceIds = assignments.stream()
                .map(FenceAssignment::getFenceId)
                .distinct()
                .collect(Collectors.toList());

        // Fetch all fences
        List<GeoFence> fences = fenceRepository.findAllById(fenceIds);

        if (fences.isEmpty()) {
            return null;
        }

        // Find the nearest fence based on distance from user's current location
        GeoFence nearestFence = fences.stream()
                .min(Comparator.comparingDouble(fence ->
                    com.tse.core_application.util.GeoMath.distanceMeters(
                        userLat, userLon,
                        fence.getCenterLat(), fence.getCenterLng()
                    )
                ))
                .orElse(null);

        return nearestFence;
    }

    /**
     * Get default fence for user (deprecated - kept for backward compatibility).
     * Use getNearestFenceForUser instead.
     */
    @Deprecated
    private GeoFence getDefaultFenceForUser(long orgId, long accountId) {
        // Expand memberships
        Set<EntityRef> entities = expandMemberships(orgId, accountId);

        // Get assignments for all entities
        List<Integer> entityTypeIds = entities.stream()
                .map(e -> e.entityTypeId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> entityIds = entities.stream()
                .map(e -> e.entityId)
                .distinct()
                .collect(Collectors.toList());

        List<FenceAssignment> assignments = assignmentRepository
                .findByOrgIdAndEntityTypeIdInAndEntityIdIn(orgId, entityTypeIds, entityIds);

        if (assignments.isEmpty()) {
            return null;
        }

        // Compute default fence with precedence: USER > TEAM > PROJECT > ORG
        FenceAssignment defaultAssignment = assignments.stream()
                .min(Comparator
                        .comparingInt((FenceAssignment a) -> precedence(a.getEntityTypeId()))
                        .thenComparing(FenceAssignment::getCreatedDatetime))
                .orElse(null);

        if (defaultAssignment == null) {
            return null;
        }

        return fenceRepository.findById(defaultAssignment.getFenceId()).orElse(null);
    }

    private Set<EntityRef> expandMemberships(long orgId, long accountId) {
        Set<EntityRef> entities = new HashSet<>();

        // Add USER entity
        entities.add(new EntityRef(EntityTypes.USER, accountId));

        // Add TEAMs
        List<Long> teamIds = membershipProvider.listTeamsForUser(orgId, accountId);
        for (Long teamId : teamIds) {
            entities.add(new EntityRef(EntityTypes.TEAM, teamId));
        }

        // Add PROJECTs
        List<Long> projectIds = membershipProvider.listProjectsForUser(orgId, accountId);
        for (Long projectId : projectIds) {
            entities.add(new EntityRef(EntityTypes.PROJECT, projectId));
        }

        // Add ORG entity
        entities.add(new EntityRef(EntityTypes.ORG, orgId));

        return entities;
    }

    private int precedence(int entityTypeId) {
        if (entityTypeId == EntityTypes.USER) return 1;
        if (entityTypeId == EntityTypes.TEAM) return 2;
        if (entityTypeId == EntityTypes.PROJECT) return 3;
        if (entityTypeId == EntityTypes.ORG) return 4;
        return 99;
    }

    private PunchResponse mapToResponse(AttendanceEvent event, String timeZone) {
        PunchResponse response = new PunchResponse();
        response.setEventId(event.getId());
        response.setAccountId(event.getAccountId());
        response.setEventKind(event.getEventKind().name());
        response.setEventSource(event.getEventSource().name());
        // Convert timestamp from server timezone to user timezone
        response.setTsUtc(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(event.getTsUtc(), timeZone).toString());
        response.setFenceId(event.getFenceId());
        response.setUnderRange(event.getUnderRange());
        response.setSuccess(event.getSuccess());
        response.setVerdict(event.getVerdict().name());
        response.setFailReason(event.getFailReason());
        response.setFlags(event.getFlags());
        return response;
    }

    private static class EntityRef {
        final int entityTypeId;
        final long entityId;

        EntityRef(int entityTypeId, long entityId) {
            this.entityTypeId = entityTypeId;
            this.entityId = entityId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityRef entityRef = (EntityRef) o;
            return entityTypeId == entityRef.entityTypeId && entityId == entityRef.entityId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityTypeId, entityId);
        }
    }
}
