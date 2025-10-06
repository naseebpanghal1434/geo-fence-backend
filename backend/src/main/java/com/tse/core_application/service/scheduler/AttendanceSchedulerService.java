package com.tse.core_application.service.scheduler;

import com.tse.core_application.constants.attendance.EventAction;
import com.tse.core_application.constants.attendance.EventKind;
import com.tse.core_application.constants.attendance.EventSource;
import com.tse.core_application.constants.attendance.IntegrityVerdict;
import com.tse.core_application.entity.attendance.AttendanceDay;
import com.tse.core_application.entity.attendance.AttendanceEvent;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.repository.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.policy.AttendancePolicyRepository;
import com.tse.core_application.service.attendance.DayRollupService;
import com.tse.core_application.service.attendance.HolidayProvider;
import com.tse.core_application.service.attendance.OfficePolicyProvider;
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
    // TODO: Inject notification service when available
    // private final NotificationService notificationService;

    public AttendanceSchedulerService(
            AttendancePolicyRepository policyRepository,
            AttendanceEventRepository eventRepository,
            AttendanceDayRepository dayRepository,
            OfficePolicyProvider officePolicyProvider,
            HolidayProvider holidayProvider,
            DayRollupService dayRollupService) {
        this.policyRepository = policyRepository;
        this.eventRepository = eventRepository;
        this.dayRepository = dayRepository;
        this.officePolicyProvider = officePolicyProvider;
        this.holidayProvider = holidayProvider;
        this.dayRollupService = dayRollupService;
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

        // Handle day boundary crossing
        LocalDateTime officeEndDateTime = today.atTime(officeEndTime);
        LocalDateTime cutoffDateTime = officeEndDateTime.plusMinutes(maxCheckoutAfterEndMin);

        // Determine which date we should process
        final LocalDate dateToProcess;
        if (now.isBefore(officeEndDateTime)) {
            // If current time is before office end, check yesterday
            dateToProcess = today.minusDays(1);
            officeEndDateTime = dateToProcess.atTime(officeEndTime);
            cutoffDateTime = officeEndDateTime.plusMinutes(maxCheckoutAfterEndMin);
        } else {
            dateToProcess = today;
        }

        // Check if current time matches cutoff time (rounded to minute)
        // This ensures the scheduler runs only ONCE per day at the exact cutoff time
        LocalTime cutoffTime = cutoffDateTime.toLocalTime();
        LocalTime currentTime = now.toLocalTime();

        if (currentTime.getHour() != cutoffTime.getHour() ||
            currentTime.getMinute() != cutoffTime.getMinute()) {
            // Not the exact cutoff minute, skip
            return;
        }

        logger.info("Cutoff time matched for orgId=" + orgId +
                   ". Processing auto-checkout. CutoffTime=" + cutoffTime +
                   ", CurrentTime=" + currentTime + ", Date=" + dateToProcess);

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
}
