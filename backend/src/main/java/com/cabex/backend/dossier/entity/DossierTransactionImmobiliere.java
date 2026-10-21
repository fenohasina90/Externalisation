package com.cabex.backend.dossier.entity;

import com.cabex.backend.demande.entity.DemandeService;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossier_transaction_immobiliere")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierTransactionImmobiliere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false, unique = true)
    private DemandeService demande;

    @Column(name = "nom_proprietaire_precedent", length = 255)
    private String nomProprietairePrecedent;

    @Column(name = "action_achat", nullable = false)
    @Builder.Default
    private Boolean actionAchat = false;

    @Column(name = "action_vente", nullable = false)
    @Builder.Default
    private Boolean actionVente = false;

    @Column(name = "action_location", nullable = false)
    @Builder.Default
    private Boolean actionLocation = false;

    @Column(name = "action_regularisation_papiers", nullable = false)
    @Builder.Default
    private Boolean actionRegularisationPapiers = false;

    @Column(name = "action_gestion_immobiliere", nullable = false)
    @Builder.Default
    private Boolean actionGestionImmobiliere = false;

    @Column(name = "action_autre", nullable = false)
    @Builder.Default
    private Boolean actionAutre = false;

    @Column(name = "action_autre_precision", length = 255)
    private String actionAutrePrecision;

    @Column(name = "objet", length = 20)
    private String objet;

    @Column(name = "objet_autre_precision", length = 255)
    private String objetAutrePrecision;

    @Column(name = "achat_mesure_m2", precision = 15, scale = 2)
    private BigDecimal achatMesureM2;

    @Column(name = "achat_position", length = 255)
    private String achatPosition;

    @Column(name = "achat_axe_emplacement", length = 255)
    private String achatAxeEmplacement;

    @Column(name = "achat_montant", precision = 15, scale = 2)
    private BigDecimal achatMontant;

    @Column(name = "achat_permis_construire")
    private Boolean achatPermisConstruire;

    @Column(name = "vente_mesure_m2", precision = 15, scale = 2)
    private BigDecimal venteMesureM2;

    @Column(name = "vente_position", length = 255)
    private String ventePosition;

    @Column(name = "vente_axe_emplacement", length = 255)
    private String venteAxeEmplacement;

    @Column(name = "vente_prise_photo")
    private Boolean ventePrisePhoto;

    @Column(name = "vente_visite")
    private Boolean venteVisite;

    @Column(name = "vente_montant", precision = 15, scale = 2)
    private BigDecimal venteMontant;

    @Column(name = "vente_permis_construire")
    private Boolean ventePermisConstruire;

    @Column(name = "regularisation_type_terrain", length = 30)
    private String regularisationTypeTerrain;

    @Column(name = "regularisation_situation_terrain", length = 30)
    private String regularisationSituationTerrain;

    @Column(name = "document_pv_reconnaissance", nullable = false)
    @Builder.Default
    private Boolean documentPvReconnaissance = false;

    @Column(name = "document_numero_demande_acquisition", nullable = false)
    @Builder.Default
    private Boolean documentNumeroDemandeAcquisition = false;

    @Column(name = "document_acte_vente_privee_communale", nullable = false)
    @Builder.Default
    private Boolean documentActeVentePriveeCommunale = false;

    @Column(name = "document_situation_juridique", nullable = false)
    @Builder.Default
    private Boolean documentSituationJuridique = false;

    @Column(name = "document_acte_notoriete", nullable = false)
    @Builder.Default
    private Boolean documentActeNotoriete = false;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "nombre_commerciaux_terrain")
    private Integer nombreCommerciauxTerrain;

    @Column(name = "commission_cn", precision = 15, scale = 2)
    private BigDecimal commissionCn;

    @Column(name = "prestation_abonnement_mensuel", nullable = false)
    @Builder.Default
    private Boolean prestationAbonnementMensuel = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
