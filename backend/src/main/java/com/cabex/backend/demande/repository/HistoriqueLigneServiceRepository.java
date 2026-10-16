package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.HistoriqueLigneService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoriqueLigneServiceRepository extends JpaRepository<HistoriqueLigneService, Integer> {

    List<HistoriqueLigneService> findByLigneServiceIdOrderByIdAsc(Integer ligneServiceId);

    Optional<HistoriqueLigneService> findFirstByLigneServiceIdOrderByIdDesc(Integer ligneServiceId);
}
