package com.tse.core_application.service.fence;

import com.tse.core_application.dto.fence.FenceCreateRequest;
import com.tse.core_application.dto.fence.FenceResponse;
import com.tse.core_application.dto.fence.FenceUpdateRequest;
import com.tse.core_application.entity.fence.GeoFence;
import com.tse.core_application.exception.FenceNotFoundException;
import com.tse.core_application.repository.fence.GeoFenceRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeoFenceService {

    private final GeoFenceRepository fenceRepository;

    public GeoFenceService(GeoFenceRepository fenceRepository) {
        this.fenceRepository = fenceRepository;
    }

    @Transactional
    public FenceResponse createFence(Long orgId, FenceCreateRequest request) {
        GeoFence fence = new GeoFence();
        fence.setOrgId(orgId);
        fence.setName(request.getName());
        fence.setLocationKind(request.getLocationKind());
        fence.setSiteCode(request.getSiteCode());
        fence.setTz(request.getTz());
        fence.setCenterLat(request.getCenterLat());
        fence.setCenterLng(request.getCenterLng());
        fence.setRadiusM(request.getRadiusM());
        fence.setIsActive(true); // Default to active for new fences

        if (request.getCreatedBy() != null) {
            fence.setCreatedBy(request.getCreatedBy());
        }

        fence = fenceRepository.save(fence);
        return FenceResponse.fromEntity(fence);
    }

    @Transactional
    public FenceResponse updateFence(Long orgId, FenceUpdateRequest request) {
        GeoFence fence = fenceRepository.findByIdAndOrgId(request.getId(), orgId)
            .orElseThrow(() -> new FenceNotFoundException(request.getId(), orgId));

        // Update fields
        fence.setName(request.getName());
        fence.setLocationKind(request.getLocationKind());
        fence.setSiteCode(request.getSiteCode());
        fence.setTz(request.getTz());
        fence.setCenterLat(request.getCenterLat());
        fence.setCenterLng(request.getCenterLng());
        fence.setRadiusM(request.getRadiusM());
        fence.setIsActive(request.getIsActive());

        if (request.getUpdatedBy() != null) {
            fence.setUpdatedBy(request.getUpdatedBy());
        }

        fence = fenceRepository.save(fence);
        return FenceResponse.fromEntity(fence);
    }

    @Transactional(readOnly = true)
    public List<FenceResponse> listFences(Long orgId, String status, String q, String siteCode) {
        Specification<GeoFence> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by orgId
            predicates.add(criteriaBuilder.equal(root.get("orgId"), orgId));

            // Filter by status (active/inactive/both)
            if (status != null && !status.equalsIgnoreCase("both")) {
                if (status.equalsIgnoreCase("active")) {
                    predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
                } else if (status.equalsIgnoreCase("inactive")) {
                    predicates.add(criteriaBuilder.isFalse(root.get("isActive")));
                }
            }

            // Filter by name search (case-insensitive)
            if (q != null && !q.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + q.toLowerCase() + "%"
                ));
            }

            // Filter by siteCode
            if (siteCode != null && !siteCode.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("siteCode"), siteCode));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<GeoFence> fences = fenceRepository.findAll(spec);
        return fences.stream()
            .map(FenceResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FenceResponse> getAllFences() {
        List<GeoFence> fences = fenceRepository.findAll();
        return fences.stream()
            .map(FenceResponse::fromEntity)
            .collect(Collectors.toList());
    }
}
