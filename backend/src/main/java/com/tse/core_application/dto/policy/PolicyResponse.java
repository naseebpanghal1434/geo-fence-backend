package com.tse.core_application.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.entity.policy.AttendancePolicy.IntegrityPosture;
import com.tse.core_application.entity.policy.AttendancePolicy.OutsideFencePolicy;

import java.time.LocalDateTime;

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

    public PolicyResponse() {
    }

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

    // Getters and Setters
    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDefaultsApplied() {
        return defaultsApplied;
    }

    public void setDefaultsApplied(Boolean defaultsApplied) {
        this.defaultsApplied = defaultsApplied;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OutsideFencePolicy getOutsideFencePolicy() {
        return outsideFencePolicy;
    }

    public void setOutsideFencePolicy(OutsideFencePolicy outsideFencePolicy) {
        this.outsideFencePolicy = outsideFencePolicy;
    }

    public IntegrityPosture getIntegrityPosture() {
        return integrityPosture;
    }

    public void setIntegrityPosture(IntegrityPosture integrityPosture) {
        this.integrityPosture = integrityPosture;
    }

    public Integer getAllowCheckinBeforeStartMin() {
        return allowCheckinBeforeStartMin;
    }

    public void setAllowCheckinBeforeStartMin(Integer allowCheckinBeforeStartMin) {
        this.allowCheckinBeforeStartMin = allowCheckinBeforeStartMin;
    }

    public Integer getLateCheckinAfterStartMin() {
        return lateCheckinAfterStartMin;
    }

    public void setLateCheckinAfterStartMin(Integer lateCheckinAfterStartMin) {
        this.lateCheckinAfterStartMin = lateCheckinAfterStartMin;
    }

    public Integer getAllowCheckoutBeforeEndMin() {
        return allowCheckoutBeforeEndMin;
    }

    public void setAllowCheckoutBeforeEndMin(Integer allowCheckoutBeforeEndMin) {
        this.allowCheckoutBeforeEndMin = allowCheckoutBeforeEndMin;
    }

    public Integer getMaxCheckoutAfterEndMin() {
        return maxCheckoutAfterEndMin;
    }

    public void setMaxCheckoutAfterEndMin(Integer maxCheckoutAfterEndMin) {
        this.maxCheckoutAfterEndMin = maxCheckoutAfterEndMin;
    }

    public Integer getNotifyBeforeShiftStartMin() {
        return notifyBeforeShiftStartMin;
    }

    public void setNotifyBeforeShiftStartMin(Integer notifyBeforeShiftStartMin) {
        this.notifyBeforeShiftStartMin = notifyBeforeShiftStartMin;
    }

    public Integer getFenceRadiusM() {
        return fenceRadiusM;
    }

    public void setFenceRadiusM(Integer fenceRadiusM) {
        this.fenceRadiusM = fenceRadiusM;
    }

    public Integer getAccuracyGateM() {
        return accuracyGateM;
    }

    public void setAccuracyGateM(Integer accuracyGateM) {
        this.accuracyGateM = accuracyGateM;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Integer cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public Integer getMaxSuccessfulPunchesPerDay() {
        return maxSuccessfulPunchesPerDay;
    }

    public void setMaxSuccessfulPunchesPerDay(Integer maxSuccessfulPunchesPerDay) {
        this.maxSuccessfulPunchesPerDay = maxSuccessfulPunchesPerDay;
    }

    public Integer getMaxFailedPunchesPerDay() {
        return maxFailedPunchesPerDay;
    }

    public void setMaxFailedPunchesPerDay(Integer maxFailedPunchesPerDay) {
        this.maxFailedPunchesPerDay = maxFailedPunchesPerDay;
    }

    public Integer getMaxWorkingHoursPerDay() {
        return maxWorkingHoursPerDay;
    }

    public void setMaxWorkingHoursPerDay(Integer maxWorkingHoursPerDay) {
        this.maxWorkingHoursPerDay = maxWorkingHoursPerDay;
    }

    public Integer getDwellInMin() {
        return dwellInMin;
    }

    public void setDwellInMin(Integer dwellInMin) {
        this.dwellInMin = dwellInMin;
    }

    public Integer getDwellOutMin() {
        return dwellOutMin;
    }

    public void setDwellOutMin(Integer dwellOutMin) {
        this.dwellOutMin = dwellOutMin;
    }

    public Boolean getAutoOutEnabled() {
        return autoOutEnabled;
    }

    public void setAutoOutEnabled(Boolean autoOutEnabled) {
        this.autoOutEnabled = autoOutEnabled;
    }

    public Integer getAutoOutDelayMin() {
        return autoOutDelayMin;
    }

    public void setAutoOutDelayMin(Integer autoOutDelayMin) {
        this.autoOutDelayMin = autoOutDelayMin;
    }

    public Integer getUndoWindowMin() {
        return undoWindowMin;
    }

    public void setUndoWindowMin(Integer undoWindowMin) {
        this.undoWindowMin = undoWindowMin;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(LocalDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(LocalDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }
}
