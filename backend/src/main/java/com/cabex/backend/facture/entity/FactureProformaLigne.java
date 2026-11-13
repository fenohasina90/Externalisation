package com.cabex.backend.facture.entity;

import com.cabex.backend.demande.entity.LigneService;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ligne d'une facture proforma (table {@code facture_proforma_ligne}).
 *
 * <p>Les lignes sont persistées à partir des lignes de la demande
 * reprises dans la proforma : chaque ligne porte sa désignation figée,
 * son prix unitaire et ses montants. La proforma est donc lisible en
 * base sans dépendre de l'état courant des lignes.</p>
 */
@Entity
@Table(name = "facture_proforma_ligne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureProformaLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_proforma_id", nullable = false)
    private Facture factureProforma;

    /** Ligne de service d'origine (nullable : traçabilité après retrait). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_service_id")
    private LigneService ligneService;

    /** Nature de la ligne : EXTERNALISATION ou DEBOURS. */
    @Column(name = "categorie", nullable = false, length = 30)
    private String categorie;

    /** Sous-type EXTERNALISATION (PRESTATION / ABONNEMENT_MENSUEL, NULL pour DEBOURS). */
    @Column(name = "type_prestation", length = 30)
    private String typePrestation;

    /** Ordre d'affichage dans la facture. */
    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    /** Désignation figée affichée (nom du service ou désignation du débours). */
    @Column(name = "designation", nullable = false, length = 255)
    private String designation;

    /** Prix unitaire affiché. */
    @Column(name = "pu", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal pu = BigDecimal.ZERO;

    /** Quantité facturée (1 pour une ligne de service). */
    @Column(name = "quantite", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantite = BigDecimal.ONE;

    /** Montant HTVA (quantité × PU). */
    @Column(name = "montant_htva", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantHtva = BigDecimal.ZERO;

    /** Remise en Ariary (0 par défaut). */
    @Column(name = "remise", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal remise = BigDecimal.ZERO;

    /** Net commercial (HTVA - remise). */
    @Column(name = "net_commercial", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netCommercial = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
