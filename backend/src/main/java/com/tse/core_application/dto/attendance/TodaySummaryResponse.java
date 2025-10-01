package com.tse.core_application.dto.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phase 6b: Response for GET /api/orgs/{orgId}/attendance/today
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TodaySummaryResponse {

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("dateKey")
    private String dateKey; // YYYY-MM-DD

    @JsonProperty("currentStatus")
    private String currentStatus; // "CHECKED_OUT", "CHECKED_IN", "ON_BREAK", "NOT_STARTED"

    @JsonProperty("firstInUtc")
    private String firstInUtc; // ISO-8601 timestamp

    @JsonProperty("lastOutUtc")
    private String lastOutUtc; // ISO-8601 timestamp

    @JsonProperty("workedSeconds")
    private Integer workedSeconds;

    @JsonProperty("breakSeconds")
    private Integer breakSeconds;

    @JsonProperty("events")
    private List<EventSummary> events;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventSummary {
        @JsonProperty("eventId")
        private Long eventId;

        @JsonProperty("eventKind")
        private String eventKind;

        @JsonProperty("tsUtc")
        private String tsUtc;

        @JsonProperty("success")
        private Boolean success;

        @JsonProperty("verdict")
        private String verdict;
    }
}
