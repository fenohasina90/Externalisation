package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.HistoriqueDemande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoriqueDemandeRepository extends JpaRepository<HistoriqueDemande, Integer> {

    List<HistoriqueDemande> findByDemandeIdOrderByCreatedAtAsc(Integer demandeId);

    List<HistoriqueDemande> findByDemandeIdOrderByIdAsc(Integer demandeId);

    Optional<HistoriqueDemande> findFirstByDemandeIdOrderByCreatedAtDesc(Integer demandeId);

    Optional<HistoriqueDemande> findFirstByDemandeIdOrderByIdDesc(Integer demandeId);

    long countByDemandeId(Integer demandeId);
}
