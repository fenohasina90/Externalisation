package com.cabex.backend.client.entity;

import com.cabex.backend.service.entity.DomaineService;
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
@Table(name = "societe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Societe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom_societe", nullable = false, length = 200)
    private String nomSociete;

    @Column(name = "activite", length = 255)
    private String activite;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "fb", length = 255)
    private String fb;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Column(name = "nombre_personnel")
    private Integer nombrePersonnel;

    @Column(name = "chiffre_affaire_mensuel", precision = 15, scale = 2)
    private BigDecimal chiffreAffaireMensuel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domaine_id", nullable = false)
    private DomaineService domaine;

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

    @OneToMany(mappedBy = "societe", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ClientResponsable> responsables = new HashSet<>();

    @OneToMany(mappedBy = "societe", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<com.cabex.backend.demande.entity.DemandeService> demandes = new HashSet<>();
}
