package com.tse.core_application.dto.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an effective fence available to a user with all contributing sources.
 */
public class EffectiveFenceDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("locationKind")
    private String locationKind;

    @JsonProperty("siteCode")
    private String siteCode;

    @JsonProperty("tz")
    private String tz;

    @JsonProperty("centerLat")
    private Double centerLat;

    @JsonProperty("centerLng")
    private Double centerLng;

    @JsonProperty("radiusM")
    private Integer radiusM;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("sources")
    private List<SourceRef> sources = new ArrayList<>();

    public EffectiveFenceDto() {
    }

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

    public String getLocationKind() {
        return locationKind;
    }

    public void setLocationKind(String locationKind) {
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

    public List<SourceRef> getSources() {
        return sources;
    }

    public void setSources(List<SourceRef> sources) {
        this.sources = sources;
    }
}
