package com.tse.core_application.DummyClasses;

import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public String getUsernameFromToken(String token) {
        // Dummy implementation - returns hardcoded username
        return "dummyuser@example.com";
    }
}
