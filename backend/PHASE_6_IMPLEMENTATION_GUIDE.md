# Phase 6 Implementation Guide

## Phase 6a ✅ COMPLETED - Core Infrastructure

### What's Included:
- ✅ Database Migration V5 (attendance_event, attendance_day tables)
- ✅ All Enums (EventKind, EventSource, EventAction, AttendanceStatus, ExceptionCode, IntegrityVerdict)
- ✅ Entity Classes (AttendanceEvent, AttendanceDay)
- ✅ Repository Interfaces (AttendanceEventRepository, AttendanceDayRepository)
- ✅ GeoMath Utility (distance calculations, fence validation)

## Phase 6b - TODO: CHECK_IN/CHECK_OUT Logic

### Components Needed:

#### 1. DTOs (`dto/attendance/`)
```java
// PunchCreateRequest.java
public class PunchCreateRequest {
    private Long accountId;
    private EventKind eventKind;
    private EventAction eventAction;
    private EventSource eventSource;
    private OffsetDateTime punchDateTime;
    private Long fenceId;
    private Double lat, lon;
    private Double accuracyM;
    // + getters/setters
}

// PunchResponse.java
public class PunchResponse {
    private Long punchEventId;
    private boolean success;
    private IntegrityVerdict verdict;
    private ExceptionCode failReason;
    // + getters/setters
}
```

#### 2. Service Providers (`service/attendance/`)

**HolidayProvider.java** (stub)
```java
@Component
public class HolidayProvider {
    public boolean isHoliday(Long orgId, LocalDate date) {
        return false; // Stub: always non-holiday
    }
}
```

**OfficePolicyProvider.java** (stub)
```java
@Component
public class OfficePolicyProvider {
    public OfficeHours getOfficeHours(Long orgId) {
        // Return default 9:30 - 18:30
        return new OfficeHours(
            LocalTime.of(9, 30),
            LocalTime.of(18, 30)
        );
    }
}
```

#### 3. Acceptance Rules (`service/attendance/AcceptanceRules.java`)

**Key Methods:**
- `validateCheckIn()` - Window validation, duplicate check
- `validateCheckOut()` - Requires check-in, window validation
- `validateGeoFence()` - Fence matching, distance check
- `validateAccuracy()` - Accuracy gate enforcement
- `checkCooldown()` - Enforce cooldown seconds
- `checkDailyCaps()` - maxSuccessfulPunchesPerDay, maxFailedPunchesPerDay

**Business Rules:**
```java
// Check-in window validation
if (now < officeStart - allowCheckinBeforeStartMin) {
    return fail(FAILED_PUNCH);
}
if (now > officeStart + lateCheckinAfterStartMin) {
    return warn(LATE_CHECKIN);
}

// Check-out validation
if (!hasCheckInToday) {
    return fail(MISSING_CHECKIN);
}
if (currentlyOnBreak) {
    return fail(BEFORE_CHECKOUT); // End break first
}

// Geo-fence validation
if (outsideFencePolicy == BLOCK && !insideFence) {
    return fail(OUTSIDE_FENCE);
}
if (outsideFencePolicy == WARN && !insideFence) {
    return warn(OUTSIDE_FENCE);
}

// Accuracy validation
if (accuracyM > accuracyGateM && integrityPosture == BLOCK) {
    return fail(LOW_ACCURACY);
}
```

#### 4. Day Rollup Service (`service/attendance/DayRollupService.java`)

**Responsibilities:**
- Compute `first_in_utc`, `last_out_utc`
- Calculate `worked_seconds` (last_out - first_in - breaks)
- Update `status`:
  - PRESENT: successful in/out pair
  - INCOMPLETE: missing checkout
  - FLAGGED: any WARN verdicts
- Store anomalies in JSONB

#### 5. Main Attendance Service (`service/attendance/AttendanceService.java`)

**Core Flow:**
```java
public PunchResponse createPunchEvent(PunchCreateRequest request) {
    // 1. Policy gate check
    policyGate.assertPolicyActive(orgId);

    // 2. Idempotency check (if header present)

    // 3. Resolve fence (if lat/lon present)
    GeoFence fence = resolveFence(request);

    // 4. Run acceptance rules
    AcceptanceResult result = acceptanceRules.validate(request, fence);

    // 5. Create AttendanceEvent
    AttendanceEvent event = new AttendanceEvent();
    event.setSuccess(result.isSuccess());
    event.setVerdict(result.getVerdict());
    event.setFailReason(result.getFailReason());
    // ... set all fields

    // 6. Save event
    eventRepository.save(event);

    // 7. Trigger day rollup (async or sync)
    if (result.isSuccess()) {
        dayRollupService.recomputeDay(orgId, accountId, dateKey);
    }

    // 8. Return response
    return new PunchResponse(event.getId(), result);
}
```

#### 6. Controller (`controller/attendance/AttendanceController.java`)

```java
@RestController
@RequestMapping("/api/orgs")
public class AttendanceController {

    @PostMapping("/{orgId}/attendance/punch")
    public ResponseEntity<PunchResponse> createPunch(
        @PathVariable Long orgId,
        @RequestBody PunchCreateRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        request.setOrgId(orgId);
        PunchResponse response = attendanceService.createPunchEvent(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orgId}/attendance/me/today")
    public ResponseEntity<TodaySummaryResponse> getTodaySummary(
        @PathVariable Long orgId,
        @RequestParam Long accountId
    ) {
        return ResponseEntity.ok(attendanceService.getTodaySummary(orgId, accountId));
    }
}
```

## Phase 6c - TODO: BREAK & PUNCHED Events

### Additional Components:

#### Break Logic in AcceptanceRules:
```java
// BREAK_START validation
if (!hasCheckInToday) return fail(MISSING_CHECKIN);
if (alreadyOnBreak) return fail(ALREADY_ON_BREAK);
if (outsideOfficeHours) return fail(OUTSIDE_OFFICE_HOURS);

// BREAK_END validation
if (!onBreak) return fail(NOT_ON_BREAK);
```

#### PUNCHED Event Handler:
```java
// In AcceptanceRules
public AcceptanceResult validatePunched(PunchCreateRequest request) {
    // 1. Fetch PunchRequest
    PunchRequest pr = punchRequestRepo.findByIdAndOrgId(requestId, orgId);
    if (!pr.exists() || pr.getState() != PENDING) {
        return fail(FAILED_PUNCH);
    }

    // 2. Check time window
    if (now < pr.getRequestedDatetime() || now >= pr.getExpiresAt()) {
        return fail(FAILED_PUNCH);
    }

    // 3. Validate user state
    if (!hasCheckInToday) return fail(MISSING_CHECKIN);
    if (onBreak) return fail(BEFORE_CHECKOUT);

    // 4. Mark request as FULFILLED
    pr.setState(FULFILLED);
    punchRequestRepo.save(pr);

    return pass();
}
```

## Testing Strategy

### Phase 6b Tests:
1. ✅ CHECK_IN inside fence, valid time → PASS
2. ✅ CHECK_IN late → WARN (LATE_CHECKIN)
3. ✅ CHECK_IN outside fence, BLOCK policy → FAIL (OUTSIDE_FENCE)
4. ✅ CHECK_IN outside fence, WARN policy → WARN
5. ✅ CHECK_OUT without check-in → FAIL (MISSING_CHECKIN)
6. ✅ CHECK_OUT valid → PASS, day status=PRESENT
7. ✅ Cooldown enforcement → FAIL (CAP_REACHED)
8. ✅ Daily caps → FAIL (CAP_REACHED)

### Phase 6c Tests:
1. ✅ BREAK_START after check-in → PASS
2. ✅ BREAK_START double → FAIL (ALREADY_ON_BREAK)
3. ✅ BREAK_END without start → FAIL (NOT_ON_BREAK)
4. ✅ PUNCHED in window → PASS, request FULFILLED
5. ✅ PUNCHED after expiry → FAIL (FAILED_PUNCH)

## Configuration Properties

Add to `application.yml`:
```yaml
attendance:
  policy:
    skip-activation-check: true  # Demo mode
  punch:
    max-past-skew-minutes: 5
    max-future-days: 30
  office:
    default-start: "09:30"
    default-end: "18:30"
    timezone: "Asia/Kolkata"
```

## Security Configuration

Already handled in Phase 6a:
```java
.antMatchers("/api/orgs/*/attendance/punch").permitAll()
.antMatchers("/api/orgs/*/attendance/me/today").permitAll()
```

## Deployment Checklist

- [ ] Phase 6b: Core punch logic implemented
- [ ] Phase 6b: Tests passing for CHECK_IN/OUT
- [ ] Phase 6c: Break logic implemented
- [ ] Phase 6c: PUNCHED event handling
- [ ] Phase 6c: All tests passing
- [ ] Performance testing (concurrent punches)
- [ ] Load testing (day rollup computation)
- [ ] Documentation updated
- [ ] Swagger annotations complete

## Estimated Effort

- **Phase 6b**: 800-1000 lines, ~2-3 hours
- **Phase 6c**: 400-600 lines, ~1-2 hours
- **Testing**: 300-400 lines, ~1 hour
- **Total**: ~1500-2000 lines, ~4-6 hours

---

**Phase 6a provides the foundation. Implement 6b next, then 6c for complete attendance tracking.**

---

## Phase 7 - Future Enhancement: Exception Analytics & Reporting

### Overview
Currently, exceptions (fail_reason) are stored and returned in individual event responses, but there's no dedicated API for viewing, filtering, or analyzing exceptions at scale. This phase will add comprehensive exception analytics capabilities.

### Current State
✅ **What Works:**
- Exceptions are properly stored in `attendance_event.fail_reason` field
- Exceptions are returned in API responses for individual events
- All 18 exception types are defined in `ExceptionCode` enum

❌ **What's Missing:**
- No API to list all exceptions across organization
- No filtering by exception type or date range
- No exception statistics or analytics
- No trend analysis or reporting

### Components to Implement

#### 1. Repository Query Methods (`AttendanceEventRepository.java`)

Add specialized queries for exception retrieval:

```java
public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, Long> {

    // Find all failed events for an organization
    List<AttendanceEvent> findByOrgIdAndSuccessFalseOrderByTsUtcDesc(Long orgId);

    // Find failed events by specific exception type
    List<AttendanceEvent> findByOrgIdAndFailReasonOrderByTsUtcDesc(
        Long orgId, String failReason);

    // Find failed events in date range
    List<AttendanceEvent> findByOrgIdAndSuccessFalseAndTsUtcBetween(
        Long orgId, LocalDateTime start, LocalDateTime end);

    // Find failed events for specific account in date range
    List<AttendanceEvent> findByOrgIdAndAccountIdAndSuccessFalseAndTsUtcBetween(
        Long orgId, Long accountId, LocalDateTime start, LocalDateTime end);

    // Count exceptions by type (for analytics)
    @Query("SELECT e.failReason, COUNT(e) FROM AttendanceEvent e " +
           "WHERE e.orgId = :orgId AND e.success = false " +
           "AND e.tsUtc BETWEEN :start AND :end " +
           "GROUP BY e.failReason " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> countExceptionsByType(
        @Param("orgId") Long orgId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);

    // Count exceptions per day (for trend analysis)
    @Query("SELECT CAST(e.tsUtc AS date), COUNT(e) FROM AttendanceEvent e " +
           "WHERE e.orgId = :orgId AND e.success = false " +
           "AND e.tsUtc BETWEEN :start AND :end " +
           "GROUP BY CAST(e.tsUtc AS date) " +
           "ORDER BY CAST(e.tsUtc AS date)")
    List<Object[]> countExceptionsByDay(
        @Param("orgId") Long orgId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);

    // Find top users with most exceptions
    @Query("SELECT e.accountId, COUNT(e) FROM AttendanceEvent e " +
           "WHERE e.orgId = :orgId AND e.success = false " +
           "AND e.tsUtc BETWEEN :start AND :end " +
           "GROUP BY e.accountId " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> findTopUsersWithExceptions(
        @Param("orgId") Long orgId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable);
}
```

#### 2. DTOs (`dto/attendance/exception/`)

**ExceptionDetailResponse.java**
```java
public class ExceptionDetailResponse {
    private Long eventId;
    private Long accountId;
    private String eventKind;
    private String exceptionCode;        // fail_reason
    private Map<String, Object> flags;   // Additional context
    private String timestamp;            // ISO format in user timezone
    private Double lat;
    private Double lon;
    private Long fenceId;
}
```

**ExceptionSummaryResponse.java**
```java
public class ExceptionSummaryResponse {
    private Long orgId;
    private Integer totalExceptions;
    private Map<String, Integer> exceptionCounts;  // Exception type -> count
    private String dateFrom;             // ISO date
    private String dateTo;               // ISO date
    private List<ExceptionTypeCount> topExceptions;
}

class ExceptionTypeCount {
    private String exceptionCode;
    private Integer count;
    private Double percentage;
}
```

**ExceptionTrendResponse.java**
```java
public class ExceptionTrendResponse {
    private Long orgId;
    private List<DailyExceptionCount> dailyCounts;
    private String dateFrom;
    private String dateTo;
}

class DailyExceptionCount {
    private String date;             // YYYY-MM-DD
    private Integer count;
    private Map<String, Integer> byType;  // Exception type breakdown
}
```

**ExceptionFilterRequest.java**
```java
public class ExceptionFilterRequest {
    private List<String> exceptionTypes;  // Optional: filter by specific types
    private Long accountId;               // Optional: filter by user
    private String dateFrom;              // Optional: ISO date
    private String dateTo;                // Optional: ISO date
    private Integer page;                 // Pagination
    private Integer size;                 // Page size
}
```

#### 3. Service Layer (`service/attendance/ExceptionAnalyticsService.java`)

```java
@Service
public class ExceptionAnalyticsService {

    private final AttendanceEventRepository eventRepository;
    private final OfficePolicyProvider officePolicyProvider;

    /**
     * Get exception summary with counts by type
     */
    public ExceptionSummaryResponse getExceptionSummary(
            Long orgId, LocalDate from, LocalDate to, String timeZone) {
        // Convert dates to datetime range
        // Query exception counts by type
        // Calculate percentages
        // Return summary
    }

    /**
     * Get detailed list of exceptions with filtering
     */
    public Page<ExceptionDetailResponse> getExceptions(
            Long orgId, ExceptionFilterRequest filter, String timeZone) {
        // Apply filters
        // Paginate results
        // Convert to response DTOs with timezone conversion
    }

    /**
     * Get exception trends over time (daily breakdown)
     */
    public ExceptionTrendResponse getExceptionTrends(
            Long orgId, LocalDate from, LocalDate to, String timeZone) {
        // Query daily exception counts
        // Break down by exception type per day
        // Return trend data
    }

    /**
     * Get top users with most exceptions
     */
    public List<UserExceptionSummary> getTopUsersWithExceptions(
            Long orgId, LocalDate from, LocalDate to, int limit) {
        // Query user exception counts
        // Sort by count descending
        // Return top N users
    }
}
```

#### 4. REST Controller (`controller/attendance/ExceptionController.java`)

```java
@RestController
@RequestMapping("/api/orgs/{orgId}/attendance/exceptions")
@Tag(name = "Exception Analytics", description = "Exception and failure analysis")
public class ExceptionController {

    private final ExceptionAnalyticsService analyticsService;

    /**
     * GET /api/orgs/{orgId}/attendance/exceptions
     * List all exceptions with filtering and pagination
     */
    @GetMapping
    @Operation(summary = "List exceptions", description = "Get filtered list of failed attendance events")
    public ResponseEntity<Object> listExceptions(
            @PathVariable Long orgId,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestHeader(name = "timeZone") String timeZone) {

        ExceptionFilterRequest filter = new ExceptionFilterRequest();
        filter.setExceptionTypes(types);
        filter.setAccountId(accountId);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setPage(page);
        filter.setSize(size);

        Page<ExceptionDetailResponse> response = analyticsService.getExceptions(
            orgId, filter, timeZone);

        return CustomResponseHandler.generateCustomResponse(
            HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    /**
     * GET /api/orgs/{orgId}/attendance/exceptions/summary
     * Get exception statistics and counts
     */
    @GetMapping("/summary")
    @Operation(summary = "Get exception summary", description = "Get aggregated exception statistics")
    public ResponseEntity<Object> getExceptionSummary(
            @PathVariable Long orgId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestHeader(name = "timeZone") String timeZone) {

        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        ExceptionSummaryResponse response = analyticsService.getExceptionSummary(
            orgId, from, to, timeZone);

        return CustomResponseHandler.generateCustomResponse(
            HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    /**
     * GET /api/orgs/{orgId}/attendance/exceptions/trends
     * Get exception trends over time
     */
    @GetMapping("/trends")
    @Operation(summary = "Get exception trends", description = "Get daily exception counts over time")
    public ResponseEntity<Object> getExceptionTrends(
            @PathVariable Long orgId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestHeader(name = "timeZone") String timeZone) {

        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        ExceptionTrendResponse response = analyticsService.getExceptionTrends(
            orgId, from, to, timeZone);

        return CustomResponseHandler.generateCustomResponse(
            HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    /**
     * GET /api/orgs/{orgId}/attendance/exceptions/top-users
     * Get users with most exceptions
     */
    @GetMapping("/top-users")
    @Operation(summary = "Get top users with exceptions", description = "Get users with highest exception counts")
    public ResponseEntity<Object> getTopUsersWithExceptions(
            @PathVariable Long orgId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestHeader(name = "timeZone") String timeZone) {

        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        List<UserExceptionSummary> response = analyticsService.getTopUsersWithExceptions(
            orgId, from, to, limit);

        return CustomResponseHandler.generateCustomResponse(
            HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }
}
```

### Use Cases

#### 1. View All Exceptions for Last Week
```bash
GET /api/orgs/10/attendance/exceptions?dateFrom=2025-10-01&dateTo=2025-10-07
```

#### 2. Filter by Exception Type
```bash
GET /api/orgs/10/attendance/exceptions?types=OUTSIDE_FENCE,LOW_ACCURACY
```

#### 3. View Exceptions for Specific User
```bash
GET /api/orgs/10/attendance/exceptions?accountId=123&dateFrom=2025-10-01
```

#### 4. Get Exception Summary Dashboard
```bash
GET /api/orgs/10/attendance/exceptions/summary?dateFrom=2025-09-01&dateTo=2025-10-01
```
Returns:
```json
{
  "totalExceptions": 245,
  "exceptionCounts": {
    "OUTSIDE_FENCE": 87,
    "LOW_ACCURACY": 56,
    "LATE_CHECKIN": 45,
    "DUP_CHECKIN": 32,
    "MISSING_CHECKIN": 25
  },
  "topExceptions": [
    {"exceptionCode": "OUTSIDE_FENCE", "count": 87, "percentage": 35.5},
    {"exceptionCode": "LOW_ACCURACY", "count": 56, "percentage": 22.9}
  ]
}
```

#### 5. View Exception Trends
```bash
GET /api/orgs/10/attendance/exceptions/trends?dateFrom=2025-09-01&dateTo=2025-09-30
```
Returns daily breakdown for chart visualization.

#### 6. Identify Problematic Users
```bash
GET /api/orgs/10/attendance/exceptions/top-users?limit=10
```
Shows top 10 users with most exceptions for intervention.

### Benefits

1. **Admin Dashboard**: Real-time visibility into attendance issues
2. **Trend Analysis**: Identify patterns (e.g., spike in OUTSIDE_FENCE on Mondays)
3. **User Coaching**: Identify users who need help with attendance app
4. **Policy Tuning**: Adjust fence radius/accuracy if too many false positives
5. **Compliance**: Track policy violations for audits
6. **Reporting**: Generate monthly/quarterly exception reports

### Estimated Effort

- **Repository Methods**: ~50 lines, 30 minutes
- **DTOs**: ~150 lines, 45 minutes
- **Service Layer**: ~300 lines, 2 hours
- **Controller**: ~200 lines, 1.5 hours
- **Testing**: ~200 lines, 1.5 hours
- **Documentation**: ~30 minutes
- **Total**: ~900 lines, ~6-7 hours

### Dependencies

- Phase 6a: ✅ Completed (database schema, entities)
- Phase 6b: ✅ Completed (exception recording in events)
- No external dependencies required

### Notes

- All queries should use proper indexing on `(org_id, success, ts_utc)` for performance
- Consider caching exception summaries for large organizations
- Implement pagination for large result sets
- Timezone conversion is critical for date range queries
- Exception trends can be used for anomaly detection in future phases
