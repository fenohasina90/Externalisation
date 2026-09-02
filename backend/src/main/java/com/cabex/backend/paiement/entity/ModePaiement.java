package com.cabex.backend.paiement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mode_paiement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModePaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
