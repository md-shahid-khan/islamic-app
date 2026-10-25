package com.codominator.angusmasail.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "imam_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImamProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mosque_id")
    private Mosque mosque;

    private String masjidName;
    private String masjidCity;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String fiqhSchool;

    @Builder.Default
    private Integer hijriOffsetDays = 0;

    @Builder.Default
    private boolean verified = false;
}
