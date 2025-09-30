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
