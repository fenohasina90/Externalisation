package com.cabex.backend.demande.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "statut_demande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatutDemande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "statut", nullable = false, unique = true, length = 50)
    private String statut;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "statut", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<HistoriqueDemande> historiques = new HashSet<>();
}
