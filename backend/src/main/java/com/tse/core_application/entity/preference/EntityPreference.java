package com.tse.core_application.entity.preference;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entity_preference", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entity_type_id", "entity_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_preference_id")
    private Long entityPreferenceId;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "is_geofencing_allowed", nullable = false)
    private Boolean isGeoFencingAllowed = false;

    @Column(name = "is_geofencing_active", nullable = false)
    private Boolean isGeoFencingActive = false;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_datetime", insertable = false)
    private LocalDateTime updatedDatetime;
}
