package com.cabex.backend.client.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_responsable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponsable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "societe_id", nullable = false)
    private Societe societe;

    @Column(name = "nom_client", nullable = false, length = 150)
    private String nomClient;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "fonction", length = 100)
    private String fonction;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
