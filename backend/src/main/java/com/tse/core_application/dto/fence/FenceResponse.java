package com.tse.core_application.dto.fence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tse.core_application.entity.fence.GeoFence;
import com.tse.core_application.entity.fence.GeoFence.LocationKind;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FenceResponse {

    private Long id;
    private Long orgId;
    private String name;
    private LocationKind locationKind;
    private String siteCode;
    private String tz;
    private Double centerLat;
    private Double centerLng;
    private Integer radiusM;
    private Boolean isActive;
    private Long createdBy;
    private OffsetDateTime createdDatetime;
    private Long updatedBy;
    private OffsetDateTime updatedDatetime;

    public FenceResponse() {
    }

    public static FenceResponse fromEntity(GeoFence fence) {
        FenceResponse response = new FenceResponse();
        response.setId(fence.getId());
        response.setOrgId(fence.getOrgId());
        response.setName(fence.getName());
        response.setLocationKind(fence.getLocationKind());
        response.setSiteCode(fence.getSiteCode());
        response.setTz(fence.getTz());
        response.setCenterLat(fence.getCenterLat());
        response.setCenterLng(fence.getCenterLng());
        response.setRadiusM(fence.getRadiusM());
        response.setIsActive(fence.getIsActive());
        response.setCreatedBy(fence.getCreatedBy());
        response.setCreatedDatetime(fence.getCreatedDatetime());
        response.setUpdatedBy(fence.getUpdatedBy());
        response.setUpdatedDatetime(fence.getUpdatedDatetime());
        return response;
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
