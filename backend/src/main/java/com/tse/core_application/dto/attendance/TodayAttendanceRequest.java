package com.tse.core_application.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * Request DTO for POST /api/orgs/{orgId}/attendance/today
 * Get attendance details for a specific user and date.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodayAttendanceRequest {

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotNull(message = "date is required")
    private String date; // yyyy-MM-dd format
}
