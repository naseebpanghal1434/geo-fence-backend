package com.tse.core_application.exception;

import org.springframework.http.HttpStatus;

public class PolicyNotFoundException extends ProblemException {

    public PolicyNotFoundException(Long orgId) {
        super(
            HttpStatus.NOT_FOUND,
            "POLICY_NOT_FOUND",
            "Policy not found",
            "No geo-fencing policy found for organization ID: " + orgId
        );
    }
}
