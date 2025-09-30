package com.tse.core_application.repository.fence;

import com.tse.core_application.entity.fence.GeoFence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeoFenceRepository extends JpaRepository<GeoFence, Long>, JpaSpecificationExecutor<GeoFence> {

    Optional<GeoFence> findByIdAndOrgId(Long id, Long orgId);
}
