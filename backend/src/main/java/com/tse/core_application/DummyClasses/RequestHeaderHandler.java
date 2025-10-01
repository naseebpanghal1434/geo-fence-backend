package com.tse.core_application.DummyClasses;

import org.springframework.stereotype.Component;

@Component
public class RequestHeaderHandler {

    public Long getAccountIdFromRequestHeader(String accountIds) {
        // Dummy implementation - returns hardcoded account ID
        return 10001L;
    }
}
