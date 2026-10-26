package com.cabex.backend.demande.entity;

import com.cabex.backend.service.entity.Service;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Ligne de service : un service du catalogue au sein d'une demande,
 * avec son tarif figé, sa période et son statut propre
 * (VALIDE / EN_ATTENTE / EN_COURS / TERMINE, NULL = en préparation).
 */
@Entity
@Table(name = "ligne_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeService demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "tarif", nullable = false, precision = 15, scale = 2)
    private BigDecimal tarif;

    @Column(name = "debut_service", nullable = false)
    private java.time.LocalDate debutService;

    @Column(name = "fin_service", nullable = false)
    private java.time.LocalDate finService;

    @Column(name = "statut", length = 50)
    private String statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private com.cabex.backend.facture.entity.Facture facture;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ligneService", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<HistoriqueLigneService> historiques = new HashSet<>();

    @OneToMany(mappedBy = "ligneService", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<com.cabex.backend.echeance.entity.ProposPaiement> plansPaiement = new HashSet<>();
}
