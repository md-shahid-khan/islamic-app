package com.codominator.angusmasail.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queries_masail", indexes = {
    @Index(name = "idx_query_imam_status", columnList = "imam_profile_id, status"),
    @Index(name = "idx_query_user", columnList = "user_id"),
    @Index(name = "idx_query_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasailQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imam_profile_id", nullable = false)
    private ImamProfile imamProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueryTag tag;

    private String userNameSnapshot;
    private String userContactSnapshot;
    private String localitySnapshot;

    @Column(length = 100)
    private String manhajSnapshot;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    private String questionAudioUrl;

    @Builder.Default
    private boolean isUrgent = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QueryStatus status = QueryStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String imamReplyText;

    private String imamReplyAudioUrl;

    @Builder.Default
    private boolean isPublicFaq = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}
