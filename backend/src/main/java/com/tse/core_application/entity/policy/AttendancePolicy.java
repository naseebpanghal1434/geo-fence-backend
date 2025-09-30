package com.tse.core_application.entity.policy;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "attendance_policy")
public class AttendancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    // Activation & posture
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "outside_fence_policy", nullable = false)
    private OutsideFencePolicy outsideFencePolicy = OutsideFencePolicy.WARN;

    @Enumerated(EnumType.STRING)
    @Column(name = "integrity_posture", nullable = false)
    private IntegrityPosture integrityPosture = IntegrityPosture.WARN;

    // Windows (minutes)
    @Column(name = "allow_checkin_before_start_min", nullable = false)
    private Integer allowCheckinBeforeStartMin = 20;

    @Column(name = "late_checkin_after_start_min", nullable = false)
    private Integer lateCheckinAfterStartMin = 30;

    @Column(name = "allow_checkout_before_end_min", nullable = false)
    private Integer allowCheckoutBeforeEndMin = 15;

    @Column(name = "max_checkout_after_end_min", nullable = false)
    private Integer maxCheckoutAfterEndMin = 60;

    @Column(name = "notify_before_shift_start_min", nullable = false)
    private Integer notifyBeforeShiftStartMin = 10;

    // Geofence & accuracy
    @Column(name = "fence_radius_m", nullable = false)
    private Integer fenceRadiusM = 150;

    @Column(name = "accuracy_gate_m", nullable = false)
    private Integer accuracyGateM = 80;

    // Punch limits & cooldowns
    @Column(name = "cooldown_seconds", nullable = false)
    private Integer cooldownSeconds = 120;

    @Column(name = "max_successful_punches_per_day", nullable = false)
    private Integer maxSuccessfulPunchesPerDay = 6;

    @Column(name = "max_failed_punches_per_day", nullable = false)
    private Integer maxFailedPunchesPerDay = 3;

    // Working hours (soft guard)
    @Column(name = "max_working_hours_per_day", nullable = false)
    private Integer maxWorkingHoursPerDay = 10;

    // Legacy placeholders
    @Column(name = "dwell_in_min", nullable = false)
    private Integer dwellInMin = 3;

    @Column(name = "dwell_out_min", nullable = false)
    private Integer dwellOutMin = 5;

    @Column(name = "auto_out_enabled", nullable = false)
    private Boolean autoOutEnabled = false;

    @Column(name = "auto_out_delay_min", nullable = false)
    private Integer autoOutDelayMin = 5;

    @Column(name = "undo_window_min", nullable = false)
    private Integer undoWindowMin = 5;

    // Audit
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private OffsetDateTime createdDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_datetime")
    private OffsetDateTime updatedDatetime;

    @PrePersist
    protected void onCreate() {
        createdDatetime = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDatetime = OffsetDateTime.now();
    }

    // Enums
    public enum OutsideFencePolicy {
        BLOCK, WARN
    }

    public enum IntegrityPosture {
        WARN, BLOCK
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
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

    public OffsetDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(OffsetDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(OffsetDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }
}
