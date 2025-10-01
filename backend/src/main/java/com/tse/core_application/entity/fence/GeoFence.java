package com.tse.core_application.entity.fence;

import javax.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "geofence")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GeoFence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_kind", nullable = false)
    private LocationKind locationKind;

    @Column(name = "site_code")
    private String siteCode;

    @Column(name = "tz")
    private String tz;

    // Circle geometry
    @Column(name = "center_lat", nullable = false)
    private Double centerLat;

    @Column(name = "center_lng", nullable = false)
    private Double centerLng;

    @Column(name = "radius_m", nullable = false)
    private Integer radiusM;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Audit fields
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

    // Enum
    public enum LocationKind {
        OFFICE, REMOTE
    }
}
