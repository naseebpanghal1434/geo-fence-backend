package com.tse.core_application.DummyClasses;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    public User getUserByUserName(String username) {
        // Dummy implementation - returns hardcoded user
        return new User(12345L, "dummyuser@example.com", username);
    }
}
