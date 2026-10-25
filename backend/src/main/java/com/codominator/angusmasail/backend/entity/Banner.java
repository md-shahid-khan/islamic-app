package com.codominator.angusmasail.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    private String title;

    private String actionUrl;

    private Integer displayOrder;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
