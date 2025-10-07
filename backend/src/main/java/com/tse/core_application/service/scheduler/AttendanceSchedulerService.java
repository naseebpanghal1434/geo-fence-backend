package com.tse.core_application.service.scheduler;

import com.tse.core_application.constants.EntityTypes;
import com.tse.core_application.constants.attendance.EventAction;
import com.tse.core_application.constants.attendance.EventKind;
import com.tse.core_application.constants.attendance.EventSource;
import com.tse.core_application.constants.attendance.IntegrityVerdict;
import com.tse.core_application.entity.attendance.AttendanceDay;
import com.tse.core_application.entity.attendance.AttendanceEvent;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.entity.punch.PunchRequest;
import com.tse.core_application.repository.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.policy.AttendancePolicyRepository;
import com.tse.core_application.repository.punch.PunchRequestRepository;
import com.tse.core_application.service.attendance.DayRollupService;
import com.tse.core_application.service.attendance.HolidayProvider;
import com.tse.core_application.service.attendance.OfficePolicyProvider;
import com.tse.core_application.service.membership.MembershipProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for scheduled attendance operations.
 * Handles shift notifications and auto-checkout.
 */
@Service
public class AttendanceSchedulerService {

    private static final Logger logger = LogManager.getLogger(AttendanceSchedulerService.class);

    private final AttendancePolicyRepository policyRepository;
    private final AttendanceEventRepository eventRepository;
    private final AttendanceDayRepository dayRepository;
    private final OfficePolicyProvider officePolicyProvider;
    private final HolidayProvider holidayProvider;
    private final DayRollupService dayRollupService;
    private final PunchRequestRepository punchRequestRepository;
    private final MembershipProvider membershipProvider;
    // TODO: Inject notification service when available
    // private final NotificationService notificationService;

    public AttendanceSchedulerService(
            AttendancePolicyRepository policyRepository,
            AttendanceEventRepository eventRepository,
            AttendanceDayRepository dayRepository,
            OfficePolicyProvider officePolicyProvider,
            HolidayProvider holidayProvider,
            DayRollupService dayRollupService,
            PunchRequestRepository punchRequestRepository,
            MembershipProvider membershipProvider) {
        this.policyRepository = policyRepository;
        this.eventRepository = eventRepository;
        this.dayRepository = dayRepository;
        this.officePolicyProvider = officePolicyProvider;
        this.holidayProvider = holidayProvider;
        this.dayRollupService = dayRollupService;
        this.punchRequestRepository = punchRequestRepository;
        this.membershipProvider = membershipProvider;
    }

    /**
     * Scheduled job: Notify users before shift start.
     * Runs every 1 minute.
     */
    @Scheduled(cron = "0 * * * * ?")  // Every minute at 0 seconds
    public void notifyBeforeShiftStartScheduler() {
        try {
            logger.info("Notify before shift start scheduler started at " + LocalDateTime.now());
            processNotifyBeforeShiftStart();
            logger.info("Notify before shift start scheduler completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in notifyBeforeShiftStartScheduler: " + e.getMessage(), e);
        }
    }

    /**
     * Scheduled job: Auto-checkout users who missed checkout.
     * Runs every 1 minute.
     */
    @Scheduled(cron = "0 * * * * ?")  // Every minute at 0 seconds
    public void autoCheckoutScheduler() {
        try {
            logger.info("Auto-checkout scheduler started at " + LocalDateTime.now());
            processAutoCheckout();
            logger.info("Auto-checkout scheduler completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in autoCheckoutScheduler: " + e.getMessage(), e);
        }
    }

    /**
     * Scheduled job: Mark missed punches for expired punch requests.
     * Runs every 1 minute.
     */
    @Scheduled(cron = "0 * * * * ?")  // Every minute at 0 seconds
    public void missedPunchScheduler() {
        try {
            logger.info("Missed punch scheduler started at " + LocalDateTime.now());
            processMissedPunches();
            logger.info("Missed punch scheduler completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in missedPunchScheduler: " + e.getMessage(), e);
        }
    }

    /**
     * Process shift start notifications for all active organizations.
     * Called by scheduler or manually via controller.
     */
    public void processNotifyBeforeShiftStart() {
        // Get all policies where geo-fencing is active
        List<AttendancePolicy> activePolicies = policyRepository.findAll().stream()
                .filter(policy -> policy.getIsActive() != null && policy.getIsActive())
                .collect(Collectors.toList());

        if (activePolicies.isEmpty()) {
            logger.info("No active attendance policies found");
            return;
        }

        logger.info("Processing shift notifications for " + activePolicies.size() + " organizations");

        // Process each organization in parallel for optimization
        activePolicies.parallelStream().forEach(policy -> {
            try {
                processOrgNotification(policy);
            } catch (Exception e) {
                logger.error("Error processing notification for orgId=" + policy.getOrgId() + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Process shift notification for a single organization.
     */
    private void processOrgNotification(AttendancePolicy policy) {
        Long orgId = policy.getOrgId();

        // Get office hours for the org
        LocalTime officeStartTime = officePolicyProvider.getOfficeStartTime(orgId);
        Integer notifyBeforeShiftStartMin = policy.getNotifyBeforeShiftStartMin();
        String timezone = officePolicyProvider.getOperationalTimezone(orgId);

        // Calculate trigger time
        LocalTime triggerTime = officeStartTime.minusMinutes(notifyBeforeShiftStartMin);

        // Get current time in org's timezone
        ZoneId zoneId = ZoneId.of(timezone);
        LocalTime currentTime = LocalTime.now(zoneId);

        // Check if current time matches trigger time (rounded to minute)
        if (currentTime.getHour() == triggerTime.getHour() &&
            currentTime.getMinute() == triggerTime.getMinute()) {

            logger.info("Trigger time matched for orgId=" + orgId +
                       ". Sending notifications. TriggerTime=" + triggerTime +
                       ", CurrentTime=" + currentTime);

            // Get all active users for this org (excluding already checked-in users)
            List<Long> usersToNotify = getUsersToNotify(orgId);

            if (!usersToNotify.isEmpty()) {
                logger.info("Sending shift notifications to " + usersToNotify.size() +
                           " users for orgId=" + orgId);

                sendShiftNotifications(orgId, usersToNotify, officeStartTime);

                // TODO: Record audit log
                logger.info("Notifications sent to users: " + usersToNotify);
            } else {
                logger.info("No users to notify for orgId=" + orgId);
            }
        }
    }

    /**
     * Get list of users to notify (exclude already checked-in users).
     */
    private List<Long> getUsersToNotify(Long orgId) {
        // TODO: Replace with actual user service integration
        // For now, this is a placeholder that gets users who haven't checked in today

        LocalDate today = LocalDate.now(ZoneId.of(officePolicyProvider.getOperationalTimezone(orgId)));

        // Get all attendance days for today in this org
        List<AttendanceDay> todayAttendance = dayRepository.findAll().stream()
                .filter(day -> day.getOrgId().equals(orgId) && day.getDateKey().equals(today))
                .collect(Collectors.toList());

        // Get users who already checked in (have firstInUtc set)
        Set<Long> checkedInUsers = todayAttendance.stream()
                .filter(day -> day.getFirstInUtc() != null)
                .map(AttendanceDay::getAccountId)
                .collect(Collectors.toSet());

        // TODO: Get all active users from org and filter out checked-in users
        // For now, returning empty list as we need integration with user service
        // Example:
        // List<Long> allActiveUsers = userService.getActiveUsersForOrg(orgId);
        // return allActiveUsers.stream()
        //     .filter(userId -> !checkedInUsers.contains(userId))
        //     .collect(Collectors.toList());

        logger.info("Users already checked in for orgId=" + orgId + ": " + checkedInUsers.size());
        return new ArrayList<>();  // Placeholder - needs user service integration
    }

    /**
     * Send shift start notifications to users.
     */
    private void sendShiftNotifications(Long orgId, List<Long> accountIds, LocalTime officeStartTime) {
        // TODO: Implement notification sending using the pattern from the example
        // This would follow the pattern:
        // 1. Create Notification entity
        // 2. Save to database
        // 3. Create notification views for each user
        // 4. Format payloads
        // 5. Send via FCM (taskService.sendPushNotification)

        String notificationMessage = String.format(
            "Reminder: Please check in. Your office hours start at %s.",
            officeStartTime.toString()
        );

        logger.info("Notification message for orgId=" + orgId + ": " + notificationMessage);
        logger.info("Would send notifications to " + accountIds.size() + " users");

        // TODO: Implement actual notification logic:
        // List<HashMap<String, String>> payloads = createNotificationPayloads(orgId, accountIds, notificationMessage);
        // notificationService.sendPushNotification(payloads);
    }

    /**
     * Process auto-checkout for all organizations.
     * Called by scheduler or manually via controller.
     */
    @Transactional
    public void processAutoCheckout() {
        // Get all active policies
        List<AttendancePolicy> activePolicies = policyRepository.findAll().stream()
                .filter(policy -> policy.getIsActive() != null && policy.getIsActive())
                .collect(Collectors.toList());

        if (activePolicies.isEmpty()) {
            logger.info("No active attendance policies found for auto-checkout");
            return;
        }

        logger.info("Processing auto-checkout for " + activePolicies.size() + " organizations");

        // Process each organization in parallel for optimization
        activePolicies.parallelStream().forEach(policy -> {
            try {
                processOrgAutoCheckout(policy);
            } catch (Exception e) {
                logger.error("Error processing auto-checkout for orgId=" + policy.getOrgId() + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Process auto-checkout for a single organization.
     * Handles:
     * - Missing checkout after maxCheckoutAfterEndMin
     * - Missing break end
     * - Respects holidays (only process if non-holiday or if check-in exists on holiday)
     * - Handles day boundary crossing (e.g., office end at 11:55 PM + 20 min grace = next day)
     */
    @Transactional
    public void processOrgAutoCheckout(AttendancePolicy policy) {
        Long orgId = policy.getOrgId();
        String timezone = officePolicyProvider.getOperationalTimezone(orgId);
        ZoneId zoneId = ZoneId.of(timezone);

        // Get office hours
        LocalTime officeEndTime = officePolicyProvider.getOfficeEndTime(orgId);
        Integer maxCheckoutAfterEndMin = policy.getMaxCheckoutAfterEndMin();

        // Calculate cutoff time for auto-checkout
        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();

        // Calculate when TODAY's office ends and its cutoff
        LocalDateTime todayOfficeEnd = today.atTime(officeEndTime);
        LocalDateTime todayCutoff = todayOfficeEnd.plusMinutes(maxCheckoutAfterEndMin);

        // Determine which date to process based on whether cutoff crosses midnight
        final LocalDate dateToProcess;
        final LocalDateTime cutoffDateTime;

        if (todayCutoff.toLocalDate().equals(today)) {
            // Cutoff is still today (no midnight crossing)
            // Example: Office ends 5 PM, grace 60 min = cutoff 6 PM (same day)
            dateToProcess = today;
            cutoffDateTime = todayCutoff;
        } else {
            // Cutoff is tomorrow (crossed midnight)
            // Example: Office ends 11:55 PM, grace 20 min = cutoff 12:15 AM (next day)
            // When it's 12:15 AM on Oct 6, we process Oct 5's data
            dateToProcess = today.minusDays(1);
            cutoffDateTime = dateToProcess.atTime(officeEndTime).plusMinutes(maxCheckoutAfterEndMin);
        }

        // Check if current time matches cutoff time (hour and minute)
        // This ensures the scheduler runs only ONCE per day at the exact cutoff time
        LocalTime cutoffTime = cutoffDateTime.toLocalTime();
        LocalTime currentTime = now.toLocalTime();

        if (currentTime.getHour() != cutoffTime.getHour() ||
            currentTime.getMinute() != cutoffTime.getMinute()) {
            // Not the exact cutoff minute, skip
            return;
        }

        logger.info("Cutoff time matched for orgId=" + orgId +
                   ". Processing auto-checkout. CutoffDateTime=" + cutoffDateTime +
                   ", CurrentDateTime=" + now + ", DateToProcess=" + dateToProcess);

        // Check if dateToProcess is a holiday
        boolean isHoliday = holidayProvider.isHoliday(orgId, dateToProcess);

        // Get attendance days that need auto-checkout (optimized query)
        // Only get records where user has checked in but not checked out
        List<AttendanceDay> dayRecords = dayRepository.findAll().stream()
                .filter(day -> day.getOrgId().equals(orgId) &&
                              day.getDateKey().equals(dateToProcess) &&
                              day.getFirstInUtc() != null &&      // Has checked in
                              day.getLastOutUtc() == null)        // Hasn't checked out yet
                .collect(Collectors.toList());

        logger.info("Found " + dayRecords.size() + " attendance records for orgId=" + orgId +
                   " on date=" + dateToProcess);

        for (AttendanceDay dayRecord : dayRecords) {
            try {
                // If it's a holiday and no check-in, skip
                if (isHoliday && dayRecord.getFirstInUtc() == null) {
                    continue;
                }

                processUserAutoCheckout(orgId, dayRecord, dateToProcess, policy);
            } catch (Exception e) {
                logger.error("Error processing auto-checkout for accountId=" + dayRecord.getAccountId() +
                           " in orgId=" + orgId + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process auto-checkout for a single user.
     */
    @Transactional
    public void processUserAutoCheckout(Long orgId, AttendanceDay dayRecord, LocalDate dateKey, AttendancePolicy policy) {
        Long accountId = dayRecord.getAccountId();

        // Get all events for this user on this day
        LocalDateTime dayStart = dateKey.atStartOfDay();
        LocalDateTime dayEnd = dateKey.plusDays(1).atStartOfDay();

        List<AttendanceEvent> events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, accountId, dayStart, dayEnd);

        if (events.isEmpty()) {
            return;
        }

        // Determine current state by walking through events
        boolean needsCheckout = false;
        boolean needsBreakEnd = false;
        EventKind lastSuccessfulEvent = null;

        for (AttendanceEvent event : events) {
            if (event.getSuccess() != null && event.getSuccess()) {
                lastSuccessfulEvent = event.getEventKind();
            }
        }

        // Determine what needs to be done
        if (lastSuccessfulEvent == EventKind.CHECK_IN) {
            needsCheckout = true;
        } else if (lastSuccessfulEvent == EventKind.BREAK_START) {
            needsBreakEnd = true;
            needsCheckout = true;  // After break end, still need checkout
        }

        // Missing check-in - can't auto-checkout
        if (dayRecord.getFirstInUtc() == null) {
            logger.info("Skipping auto-checkout for accountId=" + accountId +
                       " - no check-in found");
            return;
        }

        // Process missing break end
        if (needsBreakEnd) {
            createAutoEvent(orgId, accountId, EventKind.BREAK_END, events);
            logger.info("Created auto BREAK_END for accountId=" + accountId);

            // Refresh events after creating break end
            events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                    orgId, accountId, dayStart, dayEnd);
        }

        // Process missing checkout
        if (needsCheckout && dayRecord.getLastOutUtc() == null) {
            createAutoEvent(orgId, accountId, EventKind.CHECK_OUT, events);
            logger.info("Created auto CHECK_OUT for accountId=" + accountId);

            // Update day rollup
            events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                    orgId, accountId, dayStart, dayEnd);
            dayRollupService.updateDayRollup(orgId, accountId, dateKey, events);

            logger.info("Auto-checkout completed for accountId=" + accountId + " on date=" + dateKey);
        }
    }

    /**
     * Create an automatic event (BREAK_END or CHECK_OUT).
     */
    private void createAutoEvent(Long orgId, Long accountId, EventKind eventKind, List<AttendanceEvent> existingEvents) {
        AttendanceEvent autoEvent = new AttendanceEvent();
        autoEvent.setOrgId(orgId);
        autoEvent.setAccountId(accountId);
        autoEvent.setEventKind(eventKind);
        autoEvent.setEventSource(EventSource.MANUAL);  // Could also use a new EventSource.SYSTEM
        autoEvent.setEventAction(EventAction.AUTO);
        autoEvent.setTsUtc(LocalDateTime.now());
        autoEvent.setSuccess(true);
        autoEvent.setVerdict(IntegrityVerdict.PASS);
        autoEvent.setFlags(new HashMap<>());

        // Add flag to indicate this was auto-generated
        Map<String, Object> flags = new HashMap<>();
        flags.put("auto_checkout", true);
        flags.put("reason", "Missing " + eventKind.name().toLowerCase() + " after grace period");
        autoEvent.setFlags(flags);

        eventRepository.save(autoEvent);
    }

    /**
     * Process missed punches for expired punch requests.
     * Called by scheduler or manually via controller.
     */
    @Transactional
    public void processMissedPunches() {
        LocalDateTime now = LocalDateTime.now();

        // Get all active policies
        List<AttendancePolicy> activePolicies = policyRepository.findAll().stream()
                .filter(policy -> policy.getIsActive() != null && policy.getIsActive())
                .collect(Collectors.toList());

        if (activePolicies.isEmpty()) {
            logger.info("No active attendance policies found for missed punch processing");
            return;
        }

        logger.info("Processing missed punches for " + activePolicies.size() + " organizations");

        // Process each organization
        for (AttendancePolicy policy : activePolicies) {
            try {
                processOrgMissedPunches(policy.getOrgId(), now);
            } catch (Exception e) {
                logger.error("Error processing missed punches for orgId=" + policy.getOrgId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process missed punches for a single organization.
     * Finds all expired punch requests and marks missed punches for users who didn't respond.
     */
    @Transactional
    public void processOrgMissedPunches(Long orgId, LocalDateTime now) {
        String timezone = officePolicyProvider.getOperationalTimezone(orgId);
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDate today = LocalDate.now(zoneId);

        // Find all punch requests that have just expired (expiresAt matches current minute)
        List<PunchRequest> allRequests = punchRequestRepository.findAll();
        List<PunchRequest> expiredRequests = allRequests.stream()
                .filter(pr -> pr.getOrgId().equals(orgId))
                .filter(pr -> pr.getState() == PunchRequest.State.PENDING)
                .filter(pr -> {
                    LocalDateTime expiresAt = pr.getExpiresAt();
                    // Check if expires at current minute
                    return expiresAt.getYear() == now.getYear() &&
                           expiresAt.getMonthValue() == now.getMonthValue() &&
                           expiresAt.getDayOfMonth() == now.getDayOfMonth() &&
                           expiresAt.getHour() == now.getHour() &&
                           expiresAt.getMinute() == now.getMinute();
                })
                .collect(Collectors.toList());

        if (expiredRequests.isEmpty()) {
            return;
        }

        logger.info("Found " + expiredRequests.size() + " expired punch requests for orgId=" + orgId);

        // Process each expired request
        for (PunchRequest request : expiredRequests) {
            try {
                processExpiredPunchRequest(orgId, request, today, zoneId);

                // Mark request as EXPIRED
                request.setState(PunchRequest.State.EXPIRED);
                punchRequestRepository.save(request);

                logger.info("Marked punch request " + request.getId() + " as EXPIRED");
            } catch (Exception e) {
                logger.error("Error processing expired punch request " + request.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process a single expired punch request.
     * Resolves all account IDs based on entity type and marks missed punches.
     */
    @Transactional
    public void processExpiredPunchRequest(Long orgId, PunchRequest request, LocalDate dateKey, ZoneId zoneId) {
        // Resolve account IDs based on entity type
        Set<Long> accountIds = resolveAccountIds(orgId, request.getEntityTypeId(), request.getEntityId());

        if (accountIds.isEmpty()) {
            logger.info("No accounts found for punch request " + request.getId());
            return;
        }

        logger.info("Processing " + accountIds.size() + " accounts for punch request " + request.getId());

        // For each account, check if they missed the punch
        for (Long accountId : accountIds) {
            try {
                processMissedPunchForAccount(orgId, accountId, request, dateKey, zoneId);
            } catch (Exception e) {
                logger.error("Error processing missed punch for accountId=" + accountId +
                           " in punch request " + request.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Resolve account IDs based on entity type and entity ID.
     *
     * For USER: Returns the entity ID directly as account ID.
     * For TEAM/PROJECT/ORG: This is a simplified implementation that checks all unique account IDs
     * from attendance events in the org. In production, this should use a dedicated UserService
     * or AccountService to get all active users in the organization/team/project.
     */
    private Set<Long> resolveAccountIds(Long orgId, Integer entityTypeId, Long entityId) {
        Set<Long> accountIds = new HashSet<>();

        if (entityTypeId == EntityTypes.USER) {
            // Direct user - entity ID is the account ID
            accountIds.add(entityId);
        } else if (entityTypeId == EntityTypes.TEAM) {
            // Get all unique account IDs from attendance events for this org
            // Then filter by team membership
            Set<Long> allAccountIds = getAllAccountIdsInOrg(orgId);

            for (Long accountId : allAccountIds) {
                List<Long> teams = membershipProvider.listTeamsForUser(orgId, accountId);
                if (teams.contains(entityId)) {
                    accountIds.add(accountId);
                }
            }
        } else if (entityTypeId == EntityTypes.PROJECT) {
            // Get all unique account IDs from attendance events for this org
            // Then filter by project membership
            Set<Long> allAccountIds = getAllAccountIdsInOrg(orgId);

            for (Long accountId : allAccountIds) {
                List<Long> projects = membershipProvider.listProjectsForUser(orgId, accountId);
                if (projects.contains(entityId)) {
                    accountIds.add(accountId);
                }
            }
        } else if (entityTypeId == EntityTypes.ORG) {
            // Get all users in the organization
            accountIds = getAllAccountIdsInOrg(orgId);
        }

        return accountIds;
    }

    /**
     * Get all unique account IDs that have interacted with the attendance system for this org.
     * This includes accounts from both attendance days and attendance events.
     *
     * NOTE: This is a workaround. In production, you should use a UserService or AccountService
     * to get all active users in the organization, not just those with attendance records.
     */
    private Set<Long> getAllAccountIdsInOrg(Long orgId) {
        Set<Long> accountIds = new HashSet<>();

        // Get from attendance days
        List<AttendanceDay> allDays = dayRepository.findAll();
        accountIds.addAll(allDays.stream()
                .filter(day -> day.getOrgId().equals(orgId))
                .map(AttendanceDay::getAccountId)
                .collect(Collectors.toSet()));

        // Get from attendance events (to catch users who may have events but no day records yet)
        List<AttendanceEvent> allEvents = eventRepository.findAll();
        accountIds.addAll(allEvents.stream()
                .filter(event -> event.getOrgId().equals(orgId))
                .map(AttendanceEvent::getAccountId)
                .collect(Collectors.toSet()));

        return accountIds;
    }

    /**
     * Process missed punch for a single account.
     * Checks if user has checked in for the day and if they didn't respond to the punch request.
     */
    @Transactional
    public void processMissedPunchForAccount(Long orgId, Long accountId, PunchRequest request,
                                             LocalDate dateKey, ZoneId zoneId) {
        // Get attendance day record for this user
        Optional<AttendanceDay> dayRecordOpt = dayRepository.findByOrgIdAndAccountIdAndDateKey(orgId, accountId, dateKey);

        if (!dayRecordOpt.isPresent()) {
            // User hasn't checked in at all today, skip
            return;
        }

        AttendanceDay dayRecord = dayRecordOpt.get();

        // Check if user has checked in (has firstInUtc)
        if (dayRecord.getFirstInUtc() == null) {
            // User hasn't checked in, skip
            return;
        }

        // Get all events for this user on this day
        LocalDateTime dayStart = dateKey.atStartOfDay();
        LocalDateTime dayEnd = dateKey.plusDays(1).atStartOfDay();
        List<AttendanceEvent> events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, accountId, dayStart, dayEnd);

        // Check if user responded to this punch request
        boolean hasResponded = events.stream()
                .anyMatch(event -> {
                    Long punchRequestId = event.getPunchRequestId();
                    return punchRequestId != null && punchRequestId.equals(request.getId());
                });

        if (hasResponded) {
            // User responded to the punch request, no need to mark as missed
            logger.info("User " + accountId + " responded to punch request " + request.getId());
            return;
        }

        // User missed the punch - create a missed punch event
        createMissedPunchEvent(orgId, accountId, request, events);

        logger.info("Marked missed punch for accountId=" + accountId + " for punch request " + request.getId());
    }

    /**
     * Create a missed punch event.
     */
    private void createMissedPunchEvent(Long orgId, Long accountId, PunchRequest request,
                                       List<AttendanceEvent> existingEvents) {
        AttendanceEvent missedEvent = new AttendanceEvent();
        missedEvent.setOrgId(orgId);
        missedEvent.setAccountId(accountId);
        missedEvent.setEventKind(EventKind.PUNCHED);  // Use PUNCHED kind for missed punches
        missedEvent.setEventSource(EventSource.MANUAL);
        missedEvent.setEventAction(EventAction.AUTO);
        missedEvent.setTsUtc(LocalDateTime.now());
        missedEvent.setSuccess(false);  // Marked as unsuccessful
        missedEvent.setVerdict(IntegrityVerdict.FAIL);
        missedEvent.setPunchRequestId(request.getId());
        missedEvent.setRequesterAccountId(request.getRequesterAccountId());
        missedEvent.setFailReason("Missed punch: User did not respond to punch request within time window");

        // Add flags to indicate this was auto-generated as missed
        Map<String, Object> flags = new HashMap<>();
        flags.put("missed_punch", true);
        flags.put("punch_request_id", request.getId());
        flags.put("requested_datetime", request.getRequestedDatetime().toString());
        flags.put("expired_at", request.getExpiresAt().toString());
        missedEvent.setFlags(flags);

        eventRepository.save(missedEvent);
    }
}
