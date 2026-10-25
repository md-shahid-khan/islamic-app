package com.codominator.angusmasail.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String username;
    private String email;
    private Boolean emailVerified;
    private String phone;
    private String name;
    private String locality;
    private String city;
    private String address;
    private String preferredLanguage;
    private String bio;
    private String manhaj;
    private String role;
    private String selectedImamProfileId;
    private String selectedMasjidName;
    private String fcmToken;
    private Boolean isImamPending;
    private Boolean isImamVerified;
}
