package com.tse.core_application.dto.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Phase 6b: Response for GET /api/orgs/{orgId}/attendance/today
 */
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

    public TodaySummaryResponse() {
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getDateKey() {
        return dateKey;
    }

    public void setDateKey(String dateKey) {
        this.dateKey = dateKey;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getFirstInUtc() {
        return firstInUtc;
    }

    public void setFirstInUtc(String firstInUtc) {
        this.firstInUtc = firstInUtc;
    }

    public String getLastOutUtc() {
        return lastOutUtc;
    }

    public void setLastOutUtc(String lastOutUtc) {
        this.lastOutUtc = lastOutUtc;
    }

    public Integer getWorkedSeconds() {
        return workedSeconds;
    }

    public void setWorkedSeconds(Integer workedSeconds) {
        this.workedSeconds = workedSeconds;
    }

    public Integer getBreakSeconds() {
        return breakSeconds;
    }

    public void setBreakSeconds(Integer breakSeconds) {
        this.breakSeconds = breakSeconds;
    }

    public List<EventSummary> getEvents() {
        return events;
    }

    public void setEvents(List<EventSummary> events) {
        this.events = events;
    }

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

        public EventSummary() {
        }

        public Long getEventId() {
            return eventId;
        }

        public void setEventId(Long eventId) {
            this.eventId = eventId;
        }

        public String getEventKind() {
            return eventKind;
        }

        public void setEventKind(String eventKind) {
            this.eventKind = eventKind;
        }

        public String getTsUtc() {
            return tsUtc;
        }

        public void setTsUtc(String tsUtc) {
            this.tsUtc = tsUtc;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getVerdict() {
            return verdict;
        }

        public void setVerdict(String verdict) {
            this.verdict = verdict;
        }
    }
}
