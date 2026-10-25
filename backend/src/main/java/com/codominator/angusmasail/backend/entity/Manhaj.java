package com.codominator.angusmasail.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Manhaj {
    HANAFI("Hanafi"),
    SHAFII("Shafi'i"),
    MALIKI("Maliki"),
    HANBALI("Hanbali"),
    AHLE_SUNNAT_BARELVI("Ahle Sunnat (Barelvi)"),
    GENERAL_SUNNI("General Sunni");

    private final String displayName;

    Manhaj(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Manhaj fromValue(String value) {
        if (value == null || value.isBlank()) return GENERAL_SUNNI;
        String val = value.trim();
        for (Manhaj m : values()) {
            if (m.name().equalsIgnoreCase(val) || m.displayName.equalsIgnoreCase(val)) {
                return m;
            }
        }
        String cleaned = val.toUpperCase().replaceAll("[^A-Z]", "");
        for (Manhaj m : values()) {
            if (m.name().replaceAll("[^A-Z]", "").equals(cleaned)) {
                return m;
            }
        }
        return GENERAL_SUNNI;
    }
}
