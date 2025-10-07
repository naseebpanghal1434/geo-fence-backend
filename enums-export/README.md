# TypeScript Enums & Constants Export

This directory contains TypeScript enums and constants automatically exported from the Java backend.

## 📦 Exported Items

### Enums (10 total)

#### Attendance-related Enums
- **AttendanceStatus** - User attendance states (PRESENT, ABSENT, LEAVE, HOLIDAY, etc.)
- **EventAction** - Action types (MANUAL, AUTO)
- **EventKind** - Event types (CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END, PUNCHED)
- **EventSource** - Source of events (MANUAL, GEOFENCE, WIFI, SUPERVISOR)
- **ExceptionCode** - 18 exception codes for attendance violations
- **IntegrityVerdict** - Integrity check results (PASS, WARN, FAIL)

#### Policy-related Enums
- **AttendancePolicyOutsideFencePolicy** - Policy for outside fence (BLOCK, WARN)
- **AttendancePolicyIntegrityPosture** - Integrity enforcement (WARN, BLOCK)

#### Entity-related Enums
- **GeoFenceLocationKind** - Location types (OFFICE, REMOTE)
- **PunchRequestState** - Request states (PENDING, FULFILLED, EXPIRED, CANCELLED)

### Constants (2 total)

- **EntityTypes** - Entity type IDs (USER=1, ORG=2, PROJECT=4, TEAM=5)
- **FormattedResponse** - Response status strings (SUCCESS, ERROR, FAILED)

## 🚀 Usage in Angular

### Option 1: Copy entire directory
```bash
cp -r /root/Geo-fence/enums-export/* /path/to/angular-app/src/app/models/enums/
```

### Option 2: Import individual enums
```typescript
// Import specific enum
import { AttendanceStatus } from './models/enums/AttendanceStatus';

// Usage
const status: AttendanceStatus = AttendanceStatus.PRESENT;
```

### Option 3: Import everything via index
```typescript
// Import all at once
import {
  AttendanceStatus,
  EventKind,
  EntityTypes
} from './models/enums';

// Usage with constants
const userTypeId = EntityTypes.USER; // 1
```

## 🔄 Re-running the Export

When Java enums or constants are updated, regenerate TypeScript files:

```bash
cd /root/Geo-fence
python3 export-enums.py
```

## 📝 Type Safety Examples

### Using Enums
```typescript
import { AttendanceStatus, EventKind } from './models/enums';

interface AttendanceRecord {
  status: AttendanceStatus;
  eventType: EventKind;
  timestamp: Date;
}

const record: AttendanceRecord = {
  status: AttendanceStatus.PRESENT,
  eventType: EventKind.CHECK_IN,
  timestamp: new Date()
};
```

### Using Constants
```typescript
import { EntityTypes } from './models/enums';

function getEntityName(typeId: number): string {
  switch (typeId) {
    case EntityTypes.USER: return 'User';
    case EntityTypes.ORG: return 'Organization';
    case EntityTypes.PROJECT: return 'Project';
    case EntityTypes.TEAM: return 'Team';
    default: return 'Unknown';
  }
}
```

## ✅ Benefits

- **Type Safety**: Compile-time checking of enum values
- **Auto-completion**: IDE support for available enum values
- **Consistency**: Ensures frontend-backend alignment
- **Maintainability**: Single source of truth from Java backend
- **Documentation**: Self-documenting code with clear enum names

## 📂 Source Files

All enums/constants are exported from:
- `backend/src/main/java/com/tse/core_application/constants/`
- `backend/src/main/java/com/tse/core_application/entity/`

---

**Last Generated**: 2025-10-07
**Total Exports**: 12 files (10 enums + 2 constants)
