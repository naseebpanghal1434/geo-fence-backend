package com.tse.core_application.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.entity.policy.AttendancePolicy.IntegrityPosture;
import com.tse.core_application.entity.policy.AttendancePolicy.OutsideFencePolicy;
import com.tse.core_application.util.DateTimeUtils;

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

    private Integer punchRespondMinMinutes;
    private Integer punchRespondMaxMinutes;
    private Integer punchRespondDefaultMinutes;

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
        return fromEntity(policy, null);
    }

    // Factory method for full policy with timezone conversion
    public static PolicyResponse fromEntity(AttendancePolicy policy, String timeZone) {
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
        response.setPunchRespondMinMinutes(policy.getPunchRespondMinMinutes());
        response.setPunchRespondMaxMinutes(policy.getPunchRespondMaxMinutes());
        response.setPunchRespondDefaultMinutes(policy.getPunchRespondDefaultMinutes());
        response.setDwellInMin(policy.getDwellInMin());
        response.setDwellOutMin(policy.getDwellOutMin());
        response.setAutoOutEnabled(policy.getAutoOutEnabled());
        response.setAutoOutDelayMin(policy.getAutoOutDelayMin());
        response.setUndoWindowMin(policy.getUndoWindowMin());
        response.setCreatedBy(policy.getCreatedBy());
        // Convert timestamps from server timezone to user timezone
        if (timeZone != null) {
            response.setCreatedDatetime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(policy.getCreatedDatetime(), timeZone));
            response.setUpdatedDatetime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(policy.getUpdatedDatetime(), timeZone));
        } else {
            response.setCreatedDatetime(policy.getCreatedDatetime());
            response.setUpdatedDatetime(policy.getUpdatedDatetime());
        }
        response.setUpdatedBy(policy.getUpdatedBy());
        return response;
    }
}
