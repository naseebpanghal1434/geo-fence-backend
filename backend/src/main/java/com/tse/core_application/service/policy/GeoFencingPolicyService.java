package com.tse.core_application.service.policy;

import com.tse.core_application.dto.policy.PolicyCreateRequest;
import com.tse.core_application.dto.policy.PolicyResponse;
import com.tse.core_application.dto.policy.PolicyUpdateRequest;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.exception.PolicyNotFoundException;
import com.tse.core_application.exception.ProblemException;
import com.tse.core_application.repository.policy.AttendancePolicyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GeoFencingPolicyService {

    private final AttendancePolicyRepository policyRepository;

    public GeoFencingPolicyService(AttendancePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional
    public PolicyResponse createPolicy(Long orgId, PolicyCreateRequest request) {
        // Check if policy already exists (idempotent)
        Optional<AttendancePolicy> existing = policyRepository.findByOrgId(orgId);
        if (existing.isPresent()) {
            PolicyResponse response = new PolicyResponse();
            response.setStatus("NOOP");
            response.setOrgId(orgId);
            response.setPolicyId(existing.get().getId());
            return response;
        }

        try {
            // Create new policy with defaults
            AttendancePolicy policy = new AttendancePolicy();
            policy.setOrgId(orgId);
            if (request != null && request.getCreatedBy() != null) {
                policy.setCreatedBy(request.getCreatedBy());
            }

            policy = policyRepository.save(policy);

            PolicyResponse response = new PolicyResponse();
            response.setStatus("CREATED");
            response.setOrgId(orgId);
            response.setPolicyId(policy.getId());
            response.setDefaultsApplied(true);
            return response;

        } catch (DataIntegrityViolationException ex) {
            // Race condition: another thread created the policy
            Optional<AttendancePolicy> raceCheck = policyRepository.findByOrgId(orgId);
            if (raceCheck.isPresent()) {
                PolicyResponse response = new PolicyResponse();
                response.setStatus("NOOP");
                response.setOrgId(orgId);
                response.setPolicyId(raceCheck.get().getId());
                return response;
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(Long orgId) {
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
            .orElseThrow(() -> new PolicyNotFoundException(orgId));

        return PolicyResponse.fromEntity(policy);
    }

    @Transactional
    public PolicyResponse updatePolicy(Long orgId, PolicyUpdateRequest request) {
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
            .orElseThrow(() -> new PolicyNotFoundException(orgId));

        // Additional business validation
        if (request.getFenceRadiusM() < 30) {
            throw new ProblemException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Validation failed",
                "fenceRadiusM must be >= 30"
            );
        }

        if (request.getAccuracyGateM() < 10) {
            throw new ProblemException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Validation failed",
                "accuracyGateM must be >= 10"
            );
        }

        // Update fields
        policy.setIsActive(request.getIsActive());
        policy.setOutsideFencePolicy(request.getOutsideFencePolicy());
        policy.setIntegrityPosture(request.getIntegrityPosture());
        policy.setAllowCheckinBeforeStartMin(request.getAllowCheckinBeforeStartMin());
        policy.setLateCheckinAfterStartMin(request.getLateCheckinAfterStartMin());
        policy.setAllowCheckoutBeforeEndMin(request.getAllowCheckoutBeforeEndMin());
        policy.setMaxCheckoutAfterEndMin(request.getMaxCheckoutAfterEndMin());
        policy.setNotifyBeforeShiftStartMin(request.getNotifyBeforeShiftStartMin());
        policy.setFenceRadiusM(request.getFenceRadiusM());
        policy.setAccuracyGateM(request.getAccuracyGateM());
        policy.setCooldownSeconds(request.getCooldownSeconds());
        policy.setMaxSuccessfulPunchesPerDay(request.getMaxSuccessfulPunchesPerDay());
        policy.setMaxFailedPunchesPerDay(request.getMaxFailedPunchesPerDay());
        policy.setMaxWorkingHoursPerDay(request.getMaxWorkingHoursPerDay());
        policy.setDwellInMin(request.getDwellInMin());
        policy.setDwellOutMin(request.getDwellOutMin());
        policy.setAutoOutEnabled(request.getAutoOutEnabled());
        policy.setAutoOutDelayMin(request.getAutoOutDelayMin());
        policy.setUndoWindowMin(request.getUndoWindowMin());

        if (request.getUpdatedBy() != null) {
            policy.setUpdatedBy(request.getUpdatedBy());
        }

        policy = policyRepository.save(policy);

        PolicyResponse response = new PolicyResponse();
        response.setStatus("UPDATED");
        response.setPolicyId(policy.getId());
        response.setUpdatedAt(policy.getUpdatedDatetime());
        response.setOrgId(orgId);
        return response;
    }
}
