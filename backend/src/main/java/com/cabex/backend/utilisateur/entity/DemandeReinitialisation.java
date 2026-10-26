package com.cabex.backend.utilisateur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Demande de réinitialisation de mot de passe d'un utilisateur.
 * Le mot de passe temporaire n'est jamais stocke en clair : seul son hash
 * est enregistre sur l'utilisateur, et il doit etre change a la prochaine
 * connexion.
 */
@Entity
@Table(name = "demande_reinitialisation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeReinitialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private String statut = DemandeReinitialisationStatut.EN_ATTENTE;

    @Column(name = "motif_refus", length = 500)
    private String motifRefus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par")
    private Utilisateur traitePar;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genere_par")
    private Utilisateur generePar;

    @Column(name = "date_generation_mdp")
    private LocalDateTime dateGenerationMdp;

    @Column(name = "date_expiration_mdp")
    private LocalDateTime dateExpirationMdp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}