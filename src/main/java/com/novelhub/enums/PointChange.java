package com.novelhub.enums;

/**
 * 积分变更类型
 */
public enum PointChange {
    SUBSTRACT(0, "消耗积分"),
    ADD(1, "增加积分");

    private final int code;
    private final String description;

    PointChange(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}


