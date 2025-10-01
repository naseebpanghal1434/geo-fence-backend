package com.tse.core_application.controller.assignment;

import com.tse.core_application.dto.assignment.AssignFenceRequest;
import com.tse.core_application.dto.assignment.AssignFenceResult;
import com.tse.core_application.dto.assignment.AssignedEntitiesResponse;
import com.tse.core_application.service.assignment.FenceAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import com.tse.core_application.DummyClasses.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/orgs")
@Tag(name = "Fence Assignments", description = "Bulk fence-to-entity assignment operations")
public class FenceAssignmentController {

    private static final Logger logger = LogManager.getLogger(FenceAssignmentController.class);
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RequestHeaderHandler requestHeaderHandler;
    private final FenceAssignmentService assignmentService;

    public FenceAssignmentController(JwtUtil jwtUtil, UserService userService, RequestHeaderHandler requestHeaderHandler, FenceAssignmentService assignmentService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.requestHeaderHandler = requestHeaderHandler;
        this.assignmentService = assignmentService;
    }

    @PostMapping("/{orgId}/assignFenceToEntity")
    @Operation(summary = "Bulk assign/remove entities to/from a fence",
            description = "Assign or remove users, teams, projects, or orgs to/from a fence. " +
                    "Supports setting default fence per entity. Fence must be active.")
    public ResponseEntity<Object> assignFenceToEntity(
            @PathVariable Long orgId,
            @Valid @RequestBody AssignFenceRequest request,
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
        logger.info("Entered" + '"' + " assignFenceToEntity" + '"' + " method ...");

        try {
            AssignFenceResult result = assignmentService.assignFenceToEntity(orgId, request);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " assignFenceToEntity" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, result);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to assign fence to entity for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/{orgId}/getAssignedEntityOfFence")
    @Operation(summary = "Get assigned and unassigned entities for a fence",
            description = "Returns lists of entities assigned to the fence (with default flag and all fence IDs), " +
                    "and optionally unassigned entities from the directory.")
    public ResponseEntity<Object> getAssignedEntities(
            @PathVariable Long orgId,
            @RequestParam Long fenceId,
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
        logger.info("Entered" + '"' + " getAssignedEntities" + '"' + " method ...");

        try {
            AssignedEntitiesResponse response = assignmentService.getAssignedEntities(orgId, fenceId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAssignedEntities" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get assigned entities for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
