package com.codominator.angusmasail.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "imam_statuses", indexes = {
    @Index(name = "idx_status_expires", columnList = "expiresAt"),
    @Index(name = "idx_status_imam_expires", columnList = "imam_profile_id, expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImamStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imam_profile_id", nullable = false)
    private ImamProfile imamProfile;

    @Column(columnDefinition = "TEXT")
    private String contentText;

    private String mediaUrl;
    private String backgroundColorHex;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
