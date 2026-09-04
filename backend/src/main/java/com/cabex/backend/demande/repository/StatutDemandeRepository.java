package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.StatutDemande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatutDemandeRepository extends JpaRepository<StatutDemande, Integer> {

    Optional<StatutDemande> findByStatut(String statut);

    boolean existsByStatut(String statut);

    Page<StatutDemande> findByActif(Boolean actif, Pageable pageable);
}
