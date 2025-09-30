package com.tse.core_application.dto.policy;

import com.tse.core_application.entity.policy.AttendancePolicy.IntegrityPosture;
import com.tse.core_application.entity.policy.AttendancePolicy.OutsideFencePolicy;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class PolicyUpdateRequest {

    private Long updatedBy;

    // Activation & posture
    @NotNull
    private Boolean isActive;

    @NotNull
    private OutsideFencePolicy outsideFencePolicy;

    @NotNull
    private IntegrityPosture integrityPosture;

    // Windows (minutes)
    @NotNull
    @Min(0)
    private Integer allowCheckinBeforeStartMin;

    @NotNull
    @Min(0)
    private Integer lateCheckinAfterStartMin;

    @NotNull
    @Min(0)
    private Integer allowCheckoutBeforeEndMin;

    @NotNull
    @Min(0)
    private Integer maxCheckoutAfterEndMin;

    @NotNull
    @Min(0)
    private Integer notifyBeforeShiftStartMin;

    // Geofence & accuracy
    @NotNull
    @Min(30)
    private Integer fenceRadiusM;

    @NotNull
    @Min(10)
    private Integer accuracyGateM;

    // Punch limits & cooldowns
    @NotNull
    @Min(0)
    private Integer cooldownSeconds;

    @NotNull
    @Min(0)
    private Integer maxSuccessfulPunchesPerDay;

    @NotNull
    @Min(0)
    private Integer maxFailedPunchesPerDay;

    // Working hours (soft guard)
    @NotNull
    @Min(0)
    private Integer maxWorkingHoursPerDay;

    // Legacy placeholders
    @NotNull
    @Min(0)
    private Integer dwellInMin;

    @NotNull
    @Min(0)
    private Integer dwellOutMin;

    @NotNull
    private Boolean autoOutEnabled;

    @NotNull
    @Min(0)
    private Integer autoOutDelayMin;

    @NotNull
    @Min(0)
    private Integer undoWindowMin;

    // Getters and Setters
    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
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
}
