package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.HistoriqueDemande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueDemandeRepository extends JpaRepository<HistoriqueDemande, Integer> {

    List<HistoriqueDemande> findByDemandeIdOrderByCreatedAtAsc(Integer demandeId);

    long countByDemandeId(Integer demandeId);
}
