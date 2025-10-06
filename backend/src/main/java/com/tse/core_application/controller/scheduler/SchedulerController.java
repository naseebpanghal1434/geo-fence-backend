package com.tse.core_application.controller.scheduler;

import com.tse.core_application.service.scheduler.AttendanceSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal scheduler controller for attendance automation.
 * Not exposed to frontend - only for internal/scheduled use.
 */
@RestController
@RequestMapping("/internal/scheduler")
@Tag(name = "Scheduler", description = "Internal scheduler operations for attendance automation")
public class SchedulerController {

    private static final Logger logger = LogManager.getLogger(SchedulerController.class);
    private final AttendanceSchedulerService attendanceSchedulerService;

    public SchedulerController(AttendanceSchedulerService attendanceSchedulerService) {
        this.attendanceSchedulerService = attendanceSchedulerService;
    }

    /**
     * POST /internal/scheduler/notifyBeforeShiftStart
     * Scheduled endpoint to notify users before shift start.
     * Runs every minute via @Scheduled annotation in AttendanceSchedulerService.
     */
    @PostMapping("/notifyBeforeShiftStart")
    @Operation(summary = "Notify users before shift start",
               description = "Internal scheduler endpoint to notify users before their shift starts")
    public ResponseEntity<String> notifyBeforeShiftStart() {
        try {
            logger.info("Scheduler endpoint /notifyBeforeShiftStart called");
            attendanceSchedulerService.processNotifyBeforeShiftStart();
            return ResponseEntity.ok("Notification scheduler completed successfully");
        } catch (Exception e) {
            logger.error("Error in notifyBeforeShiftStart scheduler: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing shift notifications: " + e.getMessage());
        }
    }

    /**
     * POST /internal/scheduler/autoCheckout
     * Scheduled endpoint to auto-checkout users who missed checkout.
     * Runs every minute via @Scheduled annotation in AttendanceSchedulerService.
     */
    @PostMapping("/autoCheckout")
    @Operation(summary = "Auto-checkout users after maxCheckoutAfterEndMin",
               description = "Internal scheduler endpoint to mark users as checked out after grace period")
    public ResponseEntity<String> autoCheckout() {
        try {
            logger.info("Scheduler endpoint /autoCheckout called");
            attendanceSchedulerService.processAutoCheckout();
            return ResponseEntity.ok("Auto-checkout scheduler completed successfully");
        } catch (Exception e) {
            logger.error("Error in autoCheckout scheduler: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing auto-checkout: " + e.getMessage());
        }
    }
}
