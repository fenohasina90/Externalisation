package com.cabex.backend.facture.entity;

import com.cabex.backend.demande.entity.DemandeService;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Facture <b>proforma</b> (table {@code facture_proforma}).
 * Mode UNIQUE : une facture pour toute la demande ;
 * mode PAR_SERVICE : une facture par ligne de service.
 * Le montant proforma est modifiable tant que PROFORMA ; à la validation,
 * une ligne de {@link FactureValidee} est créée avec son propre montant
 * (qui peut différer) et sa référence XXXXXEXT/TNR/YYYY. La proforma
 * reste visible/appelable après validation.
 */
@Entity
@Table(name = "facture_proforma")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Référence PROFORMA : PRO-DDMMYYYY-XXXXX (toujours visible). */
    @Column(name = "reference_facture", nullable = false, unique = true, length = 100)
    private String referenceFacture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeService demande;

    @Column(name = "mode_facturation", nullable = false, length = 20)
    @Builder.Default
    private String modeFacturation = "PAR_SERVICE";

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "statut", nullable = false, length = 50)
    @Builder.Default
    private String statut = "PROFORMA";

    /** Facture validée correspondante (null tant que PROFORMA). */
    @OneToOne(mappedBy = "factureProforma", fetch = FetchType.LAZY)
    private FactureValidee factureValidee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "facture", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<com.cabex.backend.demande.entity.LigneService> lignes = new HashSet<>();
}
