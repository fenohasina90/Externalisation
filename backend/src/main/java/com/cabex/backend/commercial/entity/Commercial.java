package com.cabex.backend.commercial.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "commercial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commercial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom_commercial", nullable = false, length = 150)
    private String nomCommercial;

    @Column(name = "immatricule", nullable = false, unique = true, length = 100)
    private String immatricule;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
