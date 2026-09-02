package com.cabex.backend.notification.entity;

import com.cabex.backend.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "type_notification", nullable = false, length = 50)
    private String typeNotification;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "lu", nullable = false)
    @Builder.Default
    private Boolean lu = false;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;
}
