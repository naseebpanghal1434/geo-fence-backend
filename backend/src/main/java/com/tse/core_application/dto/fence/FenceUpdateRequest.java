package com.tse.core_application.dto.fence;

import com.tse.core_application.entity.fence.GeoFence.LocationKind;

import javax.validation.constraints.*;

public class FenceUpdateRequest {

    @NotNull
    private Long id;

    @NotBlank
    @Size(min = 1, max = 120)
    private String name;

    @NotNull
    private LocationKind locationKind;

    private String siteCode;

    private String tz;

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double centerLat;

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double centerLng;

    @NotNull
    @Min(30)
    private Integer radiusM;

    @NotNull
    private Boolean isActive;

    private Long updatedBy;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
