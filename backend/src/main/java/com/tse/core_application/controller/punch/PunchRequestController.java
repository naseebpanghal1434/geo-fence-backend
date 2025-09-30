package com.tse.core_application.controller.punch;

import com.tse.core_application.dto.punch.PunchRequestCreateDto;
import com.tse.core_application.dto.punch.PunchRequestViewDto;
import com.tse.core_application.service.punch.PunchRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * REST controller for managing punch requests.
 */
@RestController
@RequestMapping("/api/orgs")
@Tag(name = "Punch Requests", description = "Endpoints for creating and querying punch requests")
public class PunchRequestController {

    private final PunchRequestService punchRequestService;

    public PunchRequestController(PunchRequestService punchRequestService) {
        this.punchRequestService = punchRequestService;
    }

    @PostMapping("/{orgId}/requestPunchForEntity")
    @Operation(
            summary = "Create a punch request",
            description = "Creates a new punch request targeting a user, team, project, or org. " +
                    "The request remains pending within the specified time window."
    )
    public ResponseEntity<PunchRequestViewDto> createPunchRequest(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @Valid @RequestBody PunchRequestCreateDto request
    ) {
        PunchRequestViewDto result = punchRequestService.createPunchRequest(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{orgId}/getPendingRequest")
    @Operation(
            summary = "Get pending punch requests for accounts",
            description = "Returns all currently pending punch requests that apply to the specified account IDs. " +
                    "Membership expansion is performed (USER, TEAM, PROJECT, ORG)."
    )
    public ResponseEntity<List<PunchRequestViewDto>> getPendingRequests(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam("accountId")
            @Parameter(description = "Account IDs to query for (can be repeated)", required = true)
            List<Long> accountIds
    ) {
        List<PunchRequestViewDto> results = punchRequestService.getPendingRequestsForAccounts(orgId, accountIds);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{orgId}/getPunchRequestById")
    @Operation(
            summary = "Get punch request by ID",
            description = "Retrieves a single punch request by its ID within the specified organization."
    )
    public ResponseEntity<PunchRequestViewDto> getPunchRequestById(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam("id")
            @Parameter(description = "Punch request ID", required = true)
            Long id
    ) {
        PunchRequestViewDto result = punchRequestService.getPunchRequestById(orgId, id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{orgId}/getPendingRequestHistory")
    @Operation(
            summary = "Get pending request history",
            description = "Returns punch requests whose time window overlaps the specified date/time range " +
                    "and that apply to the given account IDs. Includes requests that were pending at some time in the range."
    )
    public ResponseEntity<List<PunchRequestViewDto>> getPendingRequestHistory(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam(value = "from", required = false)
            @Parameter(description = "Start of time range (ISO 8601 format, defaults to start of today UTC)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,

            @RequestParam(value = "to", required = false)
            @Parameter(description = "End of time range (ISO 8601 format, defaults to from + 1 day)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,

            @RequestParam("accountId")
            @Parameter(description = "Account IDs to query for (can be repeated)", required = true)
            List<Long> accountIds
    ) {
        List<PunchRequestViewDto> results = punchRequestService.getPendingRequestHistory(orgId, from, to, accountIds);
        return ResponseEntity.ok(results);
    }
}
