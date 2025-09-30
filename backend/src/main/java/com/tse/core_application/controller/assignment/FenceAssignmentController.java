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

@RestController
@RequestMapping("/api/orgs")
@Tag(name = "Fence Assignments", description = "Bulk fence-to-entity assignment operations")
public class FenceAssignmentController {

    private final FenceAssignmentService assignmentService;

    public FenceAssignmentController(FenceAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/{orgId}/assignFenceToEntity")
    @Operation(summary = "Bulk assign/remove entities to/from a fence",
            description = "Assign or remove users, teams, projects, or orgs to/from a fence. " +
                    "Supports setting default fence per entity. Fence must be active.")
    public ResponseEntity<AssignFenceResult> assignFenceToEntity(
            @PathVariable Long orgId,
            @Valid @RequestBody AssignFenceRequest request) {

        AssignFenceResult result = assignmentService.assignFenceToEntity(orgId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{orgId}/getAssignedEntityOfFence")
    @Operation(summary = "Get assigned and unassigned entities for a fence",
            description = "Returns lists of entities assigned to the fence (with default flag and all fence IDs), " +
                    "and optionally unassigned entities from the directory.")
    public ResponseEntity<AssignedEntitiesResponse> getAssignedEntities(
            @PathVariable Long orgId,
            @RequestParam Long fenceId) {

        AssignedEntitiesResponse response = assignmentService.getAssignedEntities(orgId, fenceId);
        return ResponseEntity.ok(response);
    }
}
