package com.cabex.backend.demande.entity;

import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "demande_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reference_demande", nullable = false, unique = true, length = 100)
    private String referenceDemande;

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

    @Column(name = "debut_service", nullable = false)
    private LocalDate debutService;

    @Column(name = "fin_service", nullable = false)
    private LocalDate finService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "societe_id")
    private Societe societe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_id")
    private Commercial commercial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_responsable_id")
    private ClientResponsable clientResponsable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "tarif", nullable = false, precision = 15, scale = 2)
    private BigDecimal tarif;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "demande", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<HistoriqueDemande> historiques = new HashSet<>();

    @OneToMany(mappedBy = "demande", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<com.cabex.backend.echeance.entity.ProposPaiement> proposPaiements = new HashSet<>();

    @OneToMany(mappedBy = "demande", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<com.cabex.backend.paiement.entity.Paiement> paiements = new HashSet<>();

    @OneToMany(mappedBy = "demande", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<com.cabex.backend.document.entity.PieceJointe> pieceJointes = new HashSet<>();
}
