package com.tse.core_application.controller.policy;

import com.tse.core_application.dto.policy.PolicyCreateRequest;
import com.tse.core_application.dto.policy.PolicyResponse;
import com.tse.core_application.dto.policy.PolicyUpdateRequest;
import com.tse.core_application.service.policy.GeoFencingPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/orgs")
@Tag(name = "GeoFencing Policy")
public class GeoFencingPolicyController {

    private final GeoFencingPolicyService policyService;

    public GeoFencingPolicyController(GeoFencingPolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping("/{orgId}/createGeoFencingPolicy")
    @Operation(summary = "Create geo-fencing policy for an organization (idempotent)")
    public ResponseEntity<PolicyResponse> createPolicy(
            @PathVariable Long orgId,
            @RequestBody(required = false) PolicyCreateRequest request) {

        PolicyResponse response = policyService.createPolicy(orgId, request);

        if ("CREATED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/{orgId}/getGeoFencePolicy")
    @Operation(summary = "Get geo-fencing policy for an organization")
    public ResponseEntity<PolicyResponse> getPolicy(@PathVariable Long orgId) {
        PolicyResponse response = policyService.getPolicy(orgId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orgId}/updateGeoFencePolicy")
    @Operation(summary = "Update geo-fencing policy for an organization")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable Long orgId,
            @Valid @RequestBody PolicyUpdateRequest request) {

        PolicyResponse response = policyService.updatePolicy(orgId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAllGeoFencePolicies")
    @Operation(summary = "Get all geo-fencing policies across all organizations")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        List<PolicyResponse> policies = policyService.getAllPolicies();
        return ResponseEntity.ok(policies);
    }
}
