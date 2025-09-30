package com.tse.core_application.repository.attendance;

import com.tse.core_application.entity.attendance.AttendanceDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for AttendanceDay.
 * Phase 6a: Basic CRUD operations.
 */
@Repository
public interface AttendanceDayRepository extends JpaRepository<AttendanceDay, Long> {

    Optional<AttendanceDay> findByOrgIdAndAccountIdAndDateKey(Long orgId, Long accountId, LocalDate dateKey);
}
