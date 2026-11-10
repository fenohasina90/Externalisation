package com.cabex.backend.facture.entity;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Facture <b>validée</b> (table {@code facture_validee}), créée à la
 * validation d'une proforma. Montant final (peut différer du montant
 * proforma) et référence XXXXXEXT/TNR/YYYY.
 *
 * Pour une demande DEBOURS ou ABONNEMENT_MENSUEL sans proforma distincte,
 * {@link #factureProforma} peut être nulle.
 *
 * Pour ABONNEMENT_MENSUEL, {@link #factureGenerale} peut pointer vers la
 * facture validée "générale" de l'abonnement (référence facture générale
 * validée dans l'interface).
 */
@Entity
@Table(name = "facture_validee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureValidee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Référence VALIDÉE : XXXXXEXT/TNR/YYYY. */
    @Column(name = "reference_facture_validee", nullable = false, unique = true, length = 50)
    private String referenceFactureValidee;

    /**
     * Facture proforma d'origine. NULL pour une validée sans proforma
     * (cas DEBOURS, ou ABONNEMENT_MENSUEL sans proforma distincte).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_proforma_id", unique = true)
    private Facture factureProforma;

    /**
     * Facture validée "générale" de l'abonnement. NULL si cette facture
     * est elle-même la générale, ou pour les cas simples (EXTERNALISATION
     * standard, DEBOURS).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_generale_id")
    private FactureValidee factureGenerale;

    /**
     * Ligne DEBOURS concernée. NULL pour EXTERNALISATION
     * (la ligne est dérivée de la facture proforma).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_service_id")
    private com.cabex.backend.demande.entity.LigneService ligneService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeService demande;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;

    @Column(name = "valide_le")
    private LocalDateTime valideLe;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
