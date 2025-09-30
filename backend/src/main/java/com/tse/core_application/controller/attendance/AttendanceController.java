package com.tse.core_application.controller.attendance;

import com.tse.core_application.dto.attendance.PunchCreateRequest;
import com.tse.core_application.dto.attendance.PunchResponse;
import com.tse.core_application.dto.attendance.TodaySummaryResponse;
import com.tse.core_application.service.attendance.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 6b: REST controller for attendance operations (CHECK_IN/OUT).
 */
@RestController
@RequestMapping("/api/orgs/{orgId}/attendance")
@Tag(name = "Attendance", description = "Attendance and punch operations")
public class AttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * POST /api/orgs/{orgId}/attendance/punch
     * Process a punch event (CHECK_IN or CHECK_OUT).
     */
    @PostMapping("/punch")
    @Operation(summary = "Process a punch event", description = "Process CHECK_IN or CHECK_OUT event with geofence validation")
    public ResponseEntity<PunchResponse> processPunch(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Punch request", required = true)
            @RequestBody PunchCreateRequest request) {

        logger.info("POST /api/orgs/{}/attendance/punch - accountId={}, eventKind={}",
                orgId, request.getAccountId(), request.getEventKind());

        PunchResponse response = attendanceService.processPunch(orgId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/orgs/{orgId}/attendance/punched
     * Process a PUNCHED event (supervisor-triggered punch).
     */
    @PostMapping("/punched")
    @Operation(summary = "Process a PUNCHED event", description = "Fulfill a punch request with PUNCHED event")
    public ResponseEntity<PunchResponse> processPunched(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Account ID", required = true)
            @RequestParam("accountId") Long accountId,
            @Parameter(description = "Punch Request ID", required = true)
            @RequestParam("punchRequestId") Long punchRequestId) {

        logger.info("POST /api/orgs/{}/attendance/punched - accountId={}, punchRequestId={}",
                orgId, accountId, punchRequestId);

        PunchResponse response = attendanceService.processPunchedEvent(orgId, accountId, punchRequestId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/orgs/{orgId}/attendance/today
     * Get today's summary for an account.
     */
    @GetMapping("/today")
    @Operation(summary = "Get today's attendance summary", description = "Get attendance summary for today including events and rollup")
    public ResponseEntity<TodaySummaryResponse> getTodaySummary(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Account ID", required = true)
            @RequestParam("accountId") Long accountId) {

        logger.info("GET /api/orgs/{}/attendance/today - accountId={}", orgId, accountId);

        TodaySummaryResponse response = attendanceService.getTodaySummary(orgId, accountId);

        return ResponseEntity.ok(response);
    }
}
