package com.cabex.backend.service.entity;

import com.cabex.backend.client.entity.Societe;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "domaine_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomaineService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom_domaine", nullable = false, unique = true, length = 150)
    private String nomDomaine;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "domaine", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Societe> societes = new HashSet<>();
}
