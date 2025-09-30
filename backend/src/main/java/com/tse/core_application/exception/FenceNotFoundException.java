package com.tse.core_application.exception;

import org.springframework.http.HttpStatus;

public class FenceNotFoundException extends ProblemException {

    public FenceNotFoundException(Long fenceId, Long orgId) {
        super(
            HttpStatus.NOT_FOUND,
            "FENCE_NOT_FOUND",
            "Fence not found",
            String.format("Fence with id=%d not found for organization with id=%d", fenceId, orgId)
        );
    }
}
