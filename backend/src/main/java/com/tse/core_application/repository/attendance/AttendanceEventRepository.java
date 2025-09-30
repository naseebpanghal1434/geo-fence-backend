package com.tse.core_application.repository.attendance;

import com.tse.core_application.entity.attendance.AttendanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for AttendanceEvent.
 * Phase 6a: Basic CRUD operations.
 */
@Repository
public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, Long> {

    List<AttendanceEvent> findByOrgIdAndAccountIdOrderByTsUtcDesc(Long orgId, Long accountId);

    List<AttendanceEvent> findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
            Long orgId, Long accountId, OffsetDateTime start, OffsetDateTime end);
}
