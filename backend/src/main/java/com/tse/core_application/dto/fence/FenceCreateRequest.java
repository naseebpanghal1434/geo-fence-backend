package com.tse.core_application.dto.fence;

import com.tse.core_application.entity.fence.GeoFence.LocationKind;

import javax.validation.constraints.*;

public class FenceCreateRequest {

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

    private Long createdBy;

    // Getters and Setters
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
