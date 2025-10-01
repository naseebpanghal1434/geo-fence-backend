package com.tse.core_application.DummyClasses;

public enum HttpCustomStatus {
    CUSTOM_SUCCESS(2000),
    CUSTOM_ERROR(4000);

    private final int value;

    HttpCustomStatus(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
