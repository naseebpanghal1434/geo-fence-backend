package com.tse.core_application.controller.userfence;

import com.tse.core_application.dto.userfence.UserFencesResponse;
import com.tse.core_application.service.userfence.UserFenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for retrieving effective fences available to users.
 */
@RestController
@RequestMapping("/api/orgs")
@Tag(name = "User Fences", description = "Endpoints for retrieving effective fences available to users")
public class UserFenceController {

    private final UserFenceService userFenceService;

    public UserFenceController(UserFenceService userFenceService) {
        this.userFenceService = userFenceService;
    }

    @GetMapping("/{orgId}/getUserFences")
    @Operation(
            summary = "Get effective fences for a user in an org",
            description = "Returns the union of all fences available to the user via USER, TEAM, PROJECT, and ORG assignments, " +
                    "de-duplicated with source attribution and a computed default fence."
    )
    public ResponseEntity<UserFencesResponse> getUserFences(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam("accountId")
            @Parameter(description = "User account ID", required = true)
            Long accountId,

            @RequestParam(value = "includeInactive", required = false, defaultValue = "false")
            @Parameter(description = "Whether to include inactive fences (default: false)")
            Boolean includeInactive
    ) {
        UserFencesResponse response = userFenceService.getUserFences(
                orgId,
                accountId,
                includeInactive != null ? includeInactive : false
        );
        return ResponseEntity.ok(response);
    }
}
