package com.cabex.backend.utilisateur.repository;

import com.cabex.backend.utilisateur.entity.DemandeReinitialisation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface DemandeReinitialisationRepository extends JpaRepository<DemandeReinitialisation, Long> {

    boolean existsByUtilisateurIdAndStatutIn(Integer utilisateurId, Collection<String> statuts);

    List<DemandeReinitialisation> findByUtilisateurIdAndStatutIn(Integer utilisateurId, Collection<String> statuts);

    Page<DemandeReinitialisation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<DemandeReinitialisation> findByStatutOrderByCreatedAtDesc(String statut, Pageable pageable);

    List<DemandeReinitialisation> findByStatutAndCreatedAtBefore(String statut, LocalDateTime seuil);

    List<DemandeReinitialisation> findByStatutAndDateExpirationMdpBefore(String statut, LocalDateTime seuil);
}