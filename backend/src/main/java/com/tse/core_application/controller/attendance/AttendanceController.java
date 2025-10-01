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

import com.tse.core_application.DummyClasses.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Phase 6b: REST controller for attendance operations (CHECK_IN/OUT).
 */
@RestController
@RequestMapping("/api/orgs/{orgId}/attendance")
@Tag(name = "Attendance", description = "Attendance and punch operations")
public class AttendanceController {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(AttendanceController.class);
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RequestHeaderHandler requestHeaderHandler;
    private final AttendanceService attendanceService;

    public AttendanceController(JwtUtil jwtUtil, UserService userService, RequestHeaderHandler requestHeaderHandler, AttendanceService attendanceService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.requestHeaderHandler = requestHeaderHandler;
        this.attendanceService = attendanceService;
    }

    /**
     * POST /api/orgs/{orgId}/attendance/punch
     * Process a punch event (CHECK_IN or CHECK_OUT).
     */
    @PostMapping("/punch")
    @Operation(summary = "Process a punch event", description = "Process CHECK_IN or CHECK_OUT event with geofence validation")
    public ResponseEntity<Object> processPunch(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Punch request", required = true)
            @RequestBody PunchCreateRequest request,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " processPunch" + '"' + " method ...");

        try {
            PunchResponse response = attendanceService.processPunch(orgId, request, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " processPunch" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to process punch for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    /**
     * POST /api/orgs/{orgId}/attendance/punched
     * Process a PUNCHED event (supervisor-triggered punch).
     */
    @PostMapping("/punched")
    @Operation(summary = "Process a PUNCHED event", description = "Fulfill a punch request with PUNCHED event")
    public ResponseEntity<Object> processPunched(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Account ID", required = true)
            @RequestParam("accountId") Long accountId,
            @Parameter(description = "Punch Request ID", required = true)
            @RequestParam("punchRequestId") Long punchRequestId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " processPunched" + '"' + " method ...");

        try {
            PunchResponse response = attendanceService.processPunchedEvent(orgId, accountId, punchRequestId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " processPunched" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to process punched event for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    /**
     * GET /api/orgs/{orgId}/attendance/today
     * Get today's summary for an account.
     */
    @GetMapping("/today")
    @Operation(summary = "Get today's attendance summary", description = "Get attendance summary for today including events and rollup")
    public ResponseEntity<Object> getTodaySummary(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Account ID", required = true)
            @RequestParam("accountId") Long accountId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTodaySummary" + '"' + " method ...");

        try {
            TodaySummaryResponse response = attendanceService.getTodaySummary(orgId, accountId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTodaySummary" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get today summary for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
