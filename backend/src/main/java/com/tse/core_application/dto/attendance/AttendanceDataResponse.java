package com.tse.core_application.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive response DTO for Attendance Data API.
 * Provides everything the frontend needs: summary, detailed list, grid, and drill-down.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDataResponse {

    // A) Summary section (for header chips)
    private SummarySection summary;

    // B) Detailed list rows (per user, per date)
    private List<DetailedRow> detailedRows;

    // C) Grid matrix (user × date)
    private List<GridRow> gridRows;

    // D) Drill-down data (per user, per date)
    private Map<String, DrillDownData> drillDownMap; // key: "accountId_date"

    /**
     * Summary section for the entire date range and per date.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummarySection {
        private Map<String, DateSummary> perDateSummary; // key: date (yyyy-MM-dd)
        private DateSummary overallSummary; // aggregate for the whole range
    }

    /**
     * Summary for a specific date.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateSummary {
        private Integer totalEmployees;
        private Integer present;
        private Integer absent;
        private Integer onLeave;
        private Integer onHoliday;
        private Integer partiallyPresent;
        private Integer latePresent;
        private Integer alertsCount; // WARN/FAIL events
    }

    /**
     * Detailed row for a specific user on a specific date.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedRow {
        // Identity
        private Long accountId;
        private String displayName;
        private String avatar;
        private String teamName;

        // Date
        private String date; // yyyy-MM-dd

        // Times (HH:mm:ss format in user timezone)
        private String checkInTime;  // null if missing
        private String checkOutTime; // null if missing

        // Breaks
        private Integer totalBreakMinutes;
        private List<BreakInterval> breaks;

        // Totals
        private Integer totalHoursMinutes; // total minutes worked
        private Integer totalEffortMinutes; // total effort (total - break)

        // Location
        private String primaryFenceName; // fence/site name for check-in/out

        // Status
        private String status; // PRESENT, LATE, PARTIAL, ABSENT, LEAVE, HOLIDAY

        // Flags
        private List<String> flags; // e.g., "Late check-in", "Outside fence", "Integrity warn"
    }

    /**
     * Break interval.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakInterval {
        private String startTime; // HH:mm:ss
        private String endTime;   // HH:mm:ss, null if break not ended
        private Integer durationMinutes;
    }

    /**
     * Grid row for a user across multiple dates.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GridRow {
        private Long accountId;
        private String displayName;
        private Map<String, GridCell> dateCells; // key: date (yyyy-MM-dd)
    }

    /**
     * Grid cell for a specific user on a specific date.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GridCell {
        private String status; // PRESENT, ABSENT, LEAVE, HOLIDAY, PARTIAL, LATE
        private String badge;  // optional badge code (e.g., "LATE_IN", "OUTSIDE_FENCE")
    }

    /**
     * Drill-down data for a specific user on a specific date.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrillDownData {
        private Long accountId;
        private String date; // yyyy-MM-dd

        // Computed summary for the day
        private DaySummary daySummary;

        // Full punch timeline
        private List<PunchEvent> timeline;
    }

    /**
     * Day summary in drill-down.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySummary {
        private String checkInTime;  // HH:mm:ss
        private String checkOutTime; // HH:mm:ss
        private Integer totalHoursMinutes;
        private Integer totalEffortMinutes;
        private Integer breakMinutes;
        private Integer officeHoursMinutes; // expected hours from policy
        private String notes; // any additional notes
    }

    /**
     * Punch event in drill-down timeline.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PunchEvent {
        private Long eventId;
        private String type; // CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END, PUNCHED, MISSING_CHECK_IN, MISSING_CHECK_OUT
        private String time; // HH:mm:ss
        private String attemptStatus; // SUCCESSFUL, UNSUCCESSFUL, IGNORED, MISSING
        private String locationLabel; // fence/site or "X km away"
        private Double lat;
        private Double lon;
        private Boolean withinFence;
        private String integrityVerdict; // PASS, WARN, FAIL
        private String failReason;
    }
}
