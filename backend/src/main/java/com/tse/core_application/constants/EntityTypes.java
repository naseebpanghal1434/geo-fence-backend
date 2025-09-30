package com.tse.core_application.constants;

public final class EntityTypes {
    public static final int USER = 1;
    public static final int ORG = 2;
    public static final int PROJECT = 4;
    public static final int TEAM = 5;

    private EntityTypes() {
        // Utility class
    }

    public static boolean isValid(int entityTypeId) {
        return entityTypeId == USER || entityTypeId == ORG ||
               entityTypeId == PROJECT || entityTypeId == TEAM;
    }

    public static String getTypeName(int entityTypeId) {
        switch (entityTypeId) {
            case USER: return "user";
            case ORG: return "org";
            case PROJECT: return "project";
            case TEAM: return "team";
            default: return "unknown";
        }
    }
}
