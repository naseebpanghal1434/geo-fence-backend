package com.tse.core_application.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.entity.policy.AttendancePolicy.IntegrityPosture;
import com.tse.core_application.entity.policy.AttendancePolicy.OutsideFencePolicy;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyResponse {

    private Long policyId;
    private Long orgId;
    private String status;
    private Boolean defaultsApplied;
    private LocalDateTime updatedAt;

    // Full policy fields
    private Boolean isActive;
    private OutsideFencePolicy outsideFencePolicy;
    private IntegrityPosture integrityPosture;

    private Integer allowCheckinBeforeStartMin;
    private Integer lateCheckinAfterStartMin;
    private Integer allowCheckoutBeforeEndMin;
    private Integer maxCheckoutAfterEndMin;
    private Integer notifyBeforeShiftStartMin;

    private Integer fenceRadiusM;
    private Integer accuracyGateM;

    private Integer cooldownSeconds;
    private Integer maxSuccessfulPunchesPerDay;
    private Integer maxFailedPunchesPerDay;

    private Integer maxWorkingHoursPerDay;

    private Integer dwellInMin;
    private Integer dwellOutMin;
    private Boolean autoOutEnabled;
    private Integer autoOutDelayMin;
    private Integer undoWindowMin;

    private Long createdBy;
    private LocalDateTime createdDatetime;
    private Long updatedBy;
    private LocalDateTime updatedDatetime;

    // Factory method for full policy
    public static PolicyResponse fromEntity(AttendancePolicy policy) {
        PolicyResponse response = new PolicyResponse();
        response.setPolicyId(policy.getId());
        response.setOrgId(policy.getOrgId());
        response.setIsActive(policy.getIsActive());
        response.setOutsideFencePolicy(policy.getOutsideFencePolicy());
        response.setIntegrityPosture(policy.getIntegrityPosture());
        response.setAllowCheckinBeforeStartMin(policy.getAllowCheckinBeforeStartMin());
        response.setLateCheckinAfterStartMin(policy.getLateCheckinAfterStartMin());
        response.setAllowCheckoutBeforeEndMin(policy.getAllowCheckoutBeforeEndMin());
        response.setMaxCheckoutAfterEndMin(policy.getMaxCheckoutAfterEndMin());
        response.setNotifyBeforeShiftStartMin(policy.getNotifyBeforeShiftStartMin());
        response.setFenceRadiusM(policy.getFenceRadiusM());
        response.setAccuracyGateM(policy.getAccuracyGateM());
        response.setCooldownSeconds(policy.getCooldownSeconds());
        response.setMaxSuccessfulPunchesPerDay(policy.getMaxSuccessfulPunchesPerDay());
        response.setMaxFailedPunchesPerDay(policy.getMaxFailedPunchesPerDay());
        response.setMaxWorkingHoursPerDay(policy.getMaxWorkingHoursPerDay());
        response.setDwellInMin(policy.getDwellInMin());
        response.setDwellOutMin(policy.getDwellOutMin());
        response.setAutoOutEnabled(policy.getAutoOutEnabled());
        response.setAutoOutDelayMin(policy.getAutoOutDelayMin());
        response.setUndoWindowMin(policy.getUndoWindowMin());
        response.setCreatedBy(policy.getCreatedBy());
        response.setCreatedDatetime(policy.getCreatedDatetime());
        response.setUpdatedBy(policy.getUpdatedBy());
        response.setUpdatedDatetime(policy.getUpdatedDatetime());
        return response;
    }
}
