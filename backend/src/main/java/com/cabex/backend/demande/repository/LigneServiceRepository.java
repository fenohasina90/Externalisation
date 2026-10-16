package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.LigneService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LigneServiceRepository extends JpaRepository<LigneService, Integer> {

    List<LigneService> findByDemandeId(Integer demandeId);

    List<LigneService> findByDemandeIdOrderByIdAsc(Integer demandeId);

    boolean existsByDemandeId(Integer demandeId);

    long countByDemandeId(Integer demandeId);

    List<LigneService> findByStatutInAndFinServiceBefore(Collection<String> statuts, LocalDate date);
}
