package com.tse.core_application.repository.preference;

import com.tse.core_application.entity.preference.EntityPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityPreferenceRepository extends JpaRepository<EntityPreference, Long> {

    Optional<EntityPreference> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Query("SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END FROM EntityPreference ep " +
           "WHERE ep.entityTypeId = :entityTypeId AND ep.entityId = :entityId " +
           "AND ep.isGeoFencingAllowed = true AND ep.isGeoFencingActive = true")
    boolean isGeoFencingEnabledForEntity(@Param("entityTypeId") Integer entityTypeId,
                                         @Param("entityId") Long entityId);
}
