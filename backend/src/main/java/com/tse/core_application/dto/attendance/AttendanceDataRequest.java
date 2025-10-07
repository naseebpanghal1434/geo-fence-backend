package com.tse.core_application.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for Attendance Data API.
 * Frontend sends orgId, date range, and list of account IDs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDataRequest {

    @NotNull(message = "orgId is required")
    private Long orgId;

    @NotNull(message = "fromDate is required (format: yyyy-MM-dd)")
    private String fromDate;

    @NotNull(message = "toDate is required (format: yyyy-MM-dd)")
    private String toDate;

    @NotNull(message = "accountIds is required")
    @Size(min = 1, message = "At least one accountId is required")
    private List<Long> accountIds;
}
