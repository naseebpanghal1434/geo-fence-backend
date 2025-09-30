package com.tse.core_application.entity.fence;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "geofence")
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

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private OffsetDateTime createdDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_datetime")
    private OffsetDateTime updatedDatetime;

    @PrePersist
    protected void onCreate() {
        createdDatetime = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDatetime = OffsetDateTime.now();
    }

    // Enum
    public enum LocationKind {
        OFFICE, REMOTE
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationKind getLocationKind() {
        return locationKind;
    }

    public void setLocationKind(LocationKind locationKind) {
        this.locationKind = locationKind;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    public String getTz() {
        return tz;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public Double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(Double centerLat) {
        this.centerLat = centerLat;
    }

    public Double getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(Double centerLng) {
        this.centerLng = centerLng;
    }

    public Integer getRadiusM() {
        return radiusM;
    }

    public void setRadiusM(Integer radiusM) {
        this.radiusM = radiusM;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(OffsetDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(OffsetDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }
}
