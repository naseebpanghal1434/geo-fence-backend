package com.tse.core_application.dto.policy;

import com.tse.core_application.entity.policy.AttendancePolicy.IntegrityPosture;
import com.tse.core_application.entity.policy.AttendancePolicy.OutsideFencePolicy;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

    // Punch request respond within minutes configuration
    @NotNull
    @Min(1)
    private Integer punchRespondMinMinutes;

    @NotNull
    @Min(1)
    private Integer punchRespondMaxMinutes;

    @NotNull
    @Min(1)
    private Integer punchRespondDefaultMinutes;

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
}
