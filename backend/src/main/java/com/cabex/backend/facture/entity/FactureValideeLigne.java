package com.cabex.backend.facture.entity;

import com.cabex.backend.demande.entity.LigneService;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ligne d'une facture validée (table {@code facture_validee_ligne}).
 *
 * <p>Une facture validée peut être multi-lignes (facture combinée du mois) :
 * chaque ligne porte sa désignation figée et son montant ; la somme des lignes
 * correspond au montant de la facture. Les factures validées classiques
 * (proforma validée, débours) n'ont pas de lignes.</p>
 */
@Entity
@Table(name = "facture_validee_ligne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureValideeLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_validee_id", nullable = false)
    private FactureValidee factureValidee;

    /** Ligne de service incluse (abonnement, prestation incluse, débours inclus). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_service_id", nullable = false)
    private LigneService ligneService;

    /** Désignation figée à la création de la facture (traçabilité). */
    @Column(name = "designation", nullable = false, length = 255)
    private String designation;

    /**
     * Nature de la ligne :
     * <ul>
     *   <li>{@code PRINCIPALE} : l'abonnement — seul élément payable sur cette facture ;</li>
     *   <li>{@code RAPPEL} : prestation / débours impayé mentionné à titre d'information
     *       (se règle sur sa propre facture validée, pas sur celle-ci).</li>
     * </ul>
     */
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private String type = "PRINCIPALE";

    /** Montant à facturer pour cette ligne. */
    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
