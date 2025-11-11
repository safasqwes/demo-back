package com.novelhub.enums;

/**
 * Points Type Enum
 * Defines the type of points/credits used for function calls
 */
public enum PointsType {
    TRIAL(0, "Trial/Guest", "Trial usage for guest users (no points required)"),
    FREE(1, "Free Points", "Free points (Silver Coins) - earned through daily login, tasks, etc."),
    FIXED(2, "Fixed Points", "Fixed points (Gold Coins) - purchased with real money"),
    MIXED(3, "Mixed Points", "Both free and fixed points accepted");

    private final int code;
    private final String name;
    private final String description;

    PointsType(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find PointsType by code
     * @param code Points type code (0, 1, 2, 3)
     * @return PointsType or null if not found
     */
    public static PointsType findByCode(int code) {
        for (PointsType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
