package com.tse.core_application.exception;

import org.springframework.http.HttpStatus;

public class GeoFencingAccessDeniedException extends ProblemException {

    public GeoFencingAccessDeniedException(Long orgId) {
        super(
            HttpStatus.FORBIDDEN,
            "GEOFENCING_ACCESS_DENIED",
            "Geo-fencing access denied",
            "Geo-fencing feature is not allowed or not active for organization ID: " + orgId
        );
    }

    public GeoFencingAccessDeniedException(String message) {
        super(
            HttpStatus.FORBIDDEN,
            "GEOFENCING_ACCESS_DENIED",
            "Geo-fencing access denied",
            message
        );
    }
}
