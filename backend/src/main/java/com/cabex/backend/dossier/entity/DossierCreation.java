package com.cabex.backend.dossier.entity;

import com.cabex.backend.demande.entity.DemandeService;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dossier_creation_entreprise")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierCreation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false, unique = true)
    private DemandeService demande;

    @Column(name = "forme_juridique", nullable = false, length = 20)
    private String formeJuridique;

    @Column(name = "forme_juridique_precision", length = 100)
    private String formeJuridiquePrecision;

    @Column(name = "denomination", nullable = false, length = 255)
    private String denomination;

    @Column(name = "siege_social_actuel", length = 255)
    private String siegeSocialActuel;

    @Column(name = "capital_social", precision = 15, scale = 2)
    private BigDecimal capitalSocial;

    @Column(name = "nombre_associes_declare")
    private Integer nombreAssociesDeclare;

    @Column(name = "nom_gerant", length = 150)
    private String nomGerant;

    @Column(name = "fonction_gerant", length = 100)
    private String fonctionGerant;

    @Column(name = "location_gerance", nullable = false)
    @Builder.Default
    private Boolean locationGerance = false;

    @Column(name = "secteur_activite", length = 150)
    private String secteurActivite;

    @Column(name = "autorisation_speciale", length = 255)
    private String autorisationSpeciale;

    @Column(name = "adresse_siege", length = 255)
    private String adresseSiege;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "commune", length = 100)
    private String commune;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "type_hebergement", length = 20)
    private String typeHebergement;

    @Column(name = "hebergement_precision", length = 255)
    private String hebergementPrecision;

    @Column(name = "contrat_bail")
    private Boolean contratBail;

    @Column(name = "contrat_bail_detail", length = 255)
    private String contratBailDetail;

    @Column(name = "objet_social", columnDefinition = "TEXT")
    private String objetSocial;

    @Column(name = "date_souhaitee_debut")
    private LocalDate dateSouhaiteeDebut;

    @Column(name = "regime_activite", length = 20)
    private String regimeActivite;

    @Column(name = "activites_etablissement", columnDefinition = "TEXT")
    private String activitesEtablissement;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "assujettissement_tva", length = 100)
    private String assujettissementTva;

    @Column(name = "autre_societe", length = 255)
    private String autreSociete;

    @Column(name = "situation_matrimoniale", length = 50)
    private String situationMatrimoniale;

    @Column(name = "regime_imposition", length = 100)
    private String regimeImposition;

    @OneToMany(mappedBy = "dossier", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DossierAssocie> associes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
