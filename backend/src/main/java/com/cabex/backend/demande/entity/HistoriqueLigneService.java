package com.cabex.backend.demande.entity;

import com.cabex.backend.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Historique individuel d'une ligne de service (création, modifications,
 * facturation, plan, paiements, transitions automatiques).
 */
@Entity
@Table(name = "historique_ligne_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueLigneService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_service_id", nullable = false)
    private LigneService ligneService;

    @Column(name = "statut", nullable = false, length = 50)
    private String statut;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "origine", nullable = false, length = 20)
    @Builder.Default
    private String origine = "MANUEL";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
