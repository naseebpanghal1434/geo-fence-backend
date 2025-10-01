package com.tse.core_application.controller.fence;

import com.tse.core_application.dto.fence.FenceCreateRequest;
import com.tse.core_application.dto.fence.FenceResponse;
import com.tse.core_application.dto.fence.FenceUpdateRequest;
import com.tse.core_application.service.fence.GeoFenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import com.tse.core_application.DummyClasses.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@Tag(name = "GeoFence", description = "GeoFence management APIs")
public class GeoFenceController {

    private static final Logger logger = LogManager.getLogger(GeoFenceController.class);
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RequestHeaderHandler requestHeaderHandler;
    private final GeoFenceService fenceService;

    public GeoFenceController(JwtUtil jwtUtil, UserService userService, RequestHeaderHandler requestHeaderHandler, GeoFenceService fenceService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.requestHeaderHandler = requestHeaderHandler;
        this.fenceService = fenceService;
    }

    @PostMapping("/orgs/{orgId}/createFence")
    @Operation(summary = "Create a new geo-fence for an organization")
    public ResponseEntity<Object> createFence(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Valid @RequestBody FenceCreateRequest request,
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
        logger.info("Entered" + '"' + " createFence" + '"' + " method ...");

        try {
            FenceResponse response = fenceService.createFence(orgId, request, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " createFence" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to create fence for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PutMapping("/orgs/{orgId}/updateFence")
    @Operation(summary = "Update an existing geo-fence")
    public ResponseEntity<Object> updateFence(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Valid @RequestBody FenceUpdateRequest request,
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
        logger.info("Entered" + '"' + " updateFence" + '"' + " method ...");

        try {
            FenceResponse response = fenceService.updateFence(orgId, request, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateFence" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to update fence for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/orgs/{orgId}/getFence")
    @Operation(summary = "Get geo-fences for an organization with optional filters")
    public ResponseEntity<Object> getFences(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Parameter(description = "Filter by status: active, inactive, or both") @RequestParam(required = false, defaultValue = "both") String status,
            @Parameter(description = "Search by fence name (case-insensitive)") @RequestParam(required = false) String q,
            @Parameter(description = "Filter by site code") @RequestParam(required = false) String siteCode,
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
        logger.info("Entered" + '"' + " getFences" + '"' + " method ...");

        try {
            List<FenceResponse> fences = fenceService.listFences(orgId, status, q, siteCode, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getFences" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, fences);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get fences for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/allFence")
    @Operation(summary = "Get all geo-fences across all organizations")
    public ResponseEntity<Object> getAllFences(
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
        logger.info("Entered" + '"' + " getAllFences" + '"' + " method ...");

        try {
            List<FenceResponse> fences = fenceService.getAllFences(timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllFences" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, fences);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get all fences for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
