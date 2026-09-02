package com.cabex.backend.paiement.entity;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reference_paiement", nullable = false, unique = true, length = 100)
    private String referencePaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeService demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "echeance_id")
    private EcheancePaiement echeance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mode_paiement_id", nullable = false)
    private ModePaiement modePaiement;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @Column(name = "montant_paye", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPaye;

    @Column(name = "reference_virement", length = 150)
    private String referenceVirement;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "piece_justificative", length = 255)
    private String pieceJustificative;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
