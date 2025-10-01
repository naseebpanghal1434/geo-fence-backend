package com.tse.core_application.dto.fence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tse.core_application.entity.fence.GeoFence;
import com.tse.core_application.entity.fence.GeoFence.LocationKind;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
    private LocalDateTime createdDatetime;
    private Long updatedBy;
    private LocalDateTime updatedDatetime;

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
}
