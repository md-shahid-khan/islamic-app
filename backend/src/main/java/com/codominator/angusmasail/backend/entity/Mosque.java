package com.codominator.angusmasail.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mosques", indexes = {
    @Index(name = "idx_mosque_city", columnList = "city"),
    @Index(name = "idx_mosque_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mosque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    private String address;

    @Builder.Default
    private String country = "India";

    @Builder.Default
    private String fiqhSchool = "Hanafi";

    private String imageUrl;

    @Builder.Default
    private Integer hijriOffsetDays = 0;

    @Builder.Default
    private boolean verified = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_user_id")
    private User registeredBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_imam_profile_id")
    private ImamProfile currentImam;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
