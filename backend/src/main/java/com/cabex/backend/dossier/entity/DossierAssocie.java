package com.cabex.backend.dossier.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dossier_associe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierAssocie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierCreation dossier;

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "part_sociale", length = 50)
    private String partSociale;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "contact", length = 50)
    private String contact;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
