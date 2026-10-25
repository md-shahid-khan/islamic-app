package com.codominator.angusmasail.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    ROLE_USER,
    ROLE_IMAM,
    ROLE_ADMIN;

    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null || value.isBlank()) return ROLE_USER;
        String val = value.trim().toUpperCase();
        if (!val.startsWith("ROLE_")) {
            val = "ROLE_" + val;
        }
        for (Role r : values()) {
            if (r.name().equals(val)) return r;
        }
        return ROLE_USER;
    }
}
