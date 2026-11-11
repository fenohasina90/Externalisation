package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.LigneService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LigneServiceRepository extends JpaRepository<LigneService, Integer> {

    List<LigneService> findByDemandeId(Integer demandeId);

    List<LigneService> findByDemandeIdOrderByIdAsc(Integer demandeId);

    boolean existsByDemandeId(Integer demandeId);

    long countByDemandeId(Integer demandeId);

    List<LigneService> findByStatutInAndFinServiceBefore(Collection<String> statuts, LocalDate date);

    /**
     * Charge en une requête les lignes d'une page de demandes, avec leur
     * service (nullable : DEBOURS sans service) et leur facture.
     */
    @Query("""
        SELECT l FROM LigneService l
        LEFT JOIN FETCH l.service
        LEFT JOIN FETCH l.facture
        WHERE l.demande.id IN :demandeIds
        ORDER BY l.demande.id ASC, l.id ASC
        """)
    List<LigneService> findByDemandeIdInWithService(
            @Param("demandeIds") Collection<Integer> demandeIds);

}
