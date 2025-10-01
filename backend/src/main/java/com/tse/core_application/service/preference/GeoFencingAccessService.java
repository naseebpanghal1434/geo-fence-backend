package com.tse.core_application.service.preference;

import com.tse.core_application.constants.EntityTypes;
import com.tse.core_application.exception.GeoFencingAccessDeniedException;
import com.tse.core_application.repository.preference.EntityPreferenceRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class GeoFencingAccessService {

    private static final Logger logger = LogManager.getLogger(GeoFencingAccessService.class);

    private final EntityPreferenceRepository entityPreferenceRepository;

    public GeoFencingAccessService(EntityPreferenceRepository entityPreferenceRepository) {
        this.entityPreferenceRepository = entityPreferenceRepository;
    }

    /**
     * Validates if geo-fencing is allowed and active for the given organization.
     * For ORG entity type (entityTypeId = 2), checks if both isGeoFencingAllowed and isGeoFencingActive are true.
     *
     * @param orgId The organization ID to check
     * @throws GeoFencingAccessDeniedException if geo-fencing is not allowed or not active
     */
    public void validateGeoFencingAccess(Long orgId) {
        logger.debug("Validating geo-fencing access for orgId: {}", orgId);

        boolean isEnabled = entityPreferenceRepository.isGeoFencingEnabledForEntity(EntityTypes.ORG, orgId);

        if (!isEnabled) {
            logger.warn("Geo-fencing access denied for orgId: {}. Feature is not allowed or not active.", orgId);
            throw new GeoFencingAccessDeniedException(orgId);
        }

        logger.debug("Geo-fencing access validated successfully for orgId: {}", orgId);
    }

    /**
     * Checks if geo-fencing is enabled for the given organization without throwing exception.
     *
     * @param orgId The organization ID to check
     * @return true if geo-fencing is both allowed and active, false otherwise
     */
    public boolean isGeoFencingEnabled(Long orgId) {
        return entityPreferenceRepository.isGeoFencingEnabledForEntity(EntityTypes.ORG, orgId);
    }
}
