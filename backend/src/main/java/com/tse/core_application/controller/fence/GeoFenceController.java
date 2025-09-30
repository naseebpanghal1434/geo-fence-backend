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

@RestController
@RequestMapping("/api")
@Tag(name = "GeoFence", description = "GeoFence management APIs")
public class GeoFenceController {

    private final GeoFenceService fenceService;

    public GeoFenceController(GeoFenceService fenceService) {
        this.fenceService = fenceService;
    }

    @PostMapping("/orgs/{orgId}/createFence")
    @Operation(summary = "Create a new geo-fence for an organization")
    public ResponseEntity<FenceResponse> createFence(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Valid @RequestBody FenceCreateRequest request) {

        FenceResponse response = fenceService.createFence(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/orgs/{orgId}/updateFence")
    @Operation(summary = "Update an existing geo-fence")
    public ResponseEntity<FenceResponse> updateFence(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Valid @RequestBody FenceUpdateRequest request) {

        FenceResponse response = fenceService.updateFence(orgId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orgs/{orgId}/getFence")
    @Operation(summary = "Get geo-fences for an organization with optional filters")
    public ResponseEntity<List<FenceResponse>> getFences(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Parameter(description = "Filter by status: active, inactive, or both") @RequestParam(required = false, defaultValue = "both") String status,
            @Parameter(description = "Search by fence name (case-insensitive)") @RequestParam(required = false) String q,
            @Parameter(description = "Filter by site code") @RequestParam(required = false) String siteCode) {

        List<FenceResponse> fences = fenceService.listFences(orgId, status, q, siteCode);
        return ResponseEntity.ok(fences);
    }

    @GetMapping("/allFence")
    @Operation(summary = "Get all geo-fences across all organizations")
    public ResponseEntity<List<FenceResponse>> getAllFences() {
        List<FenceResponse> fences = fenceService.getAllFences();
        return ResponseEntity.ok(fences);
    }
}
