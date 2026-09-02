package com.cabex.backend.audit.entity;

import com.cabex.backend.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    @Column(name = "entite", length = 100)
    private String entite;

    @Column(name = "entite_id")
    private Long entiteId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancienne_valeur", columnDefinition = "json")
    private String ancienneValeur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelle_valeur", columnDefinition = "json")
    private String nouvelleValeur;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
