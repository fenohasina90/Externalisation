package com.cabex.backend.service.repository;

import com.cabex.backend.service.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Integer> {

    boolean existsByNomService(String nomService);

    Optional<Service> findByCodeService(String codeService);

    boolean existsByCodeService(String codeService);

    long countByActifTrue();

    /** Tous les codes, pour le calcul du suivant (SRV-0001, SRV-0002, …). */
    @Query("SELECT s.codeService FROM Service s")
    java.util.List<String> findAllCodes();

    @Query("""
        SELECT s FROM Service s
        WHERE (:recherche IS NULL
               OR LOWER(s.nomService) LIKE LOWER(CONCAT('%', :recherche, '%'))
               OR LOWER(COALESCE(s.codeService, '')) LIKE LOWER(CONCAT('%', :recherche, '%')))
          AND (:actif IS NULL OR s.actif = :actif)
        """)
    Page<Service> rechercher(@Param("recherche") String recherche,
                             @Param("actif") Boolean actif,
                             Pageable pageable);

    @Query("""
        SELECT s FROM Service s
        WHERE LOWER(s.nomService) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY s.nomService
        """)
    List<Service> rechercherGlobale(@Param("q") String q, Pageable pageable);

    /** Services actifs et non archivés proposés dans les filtres du tableau de bord. */
    @Query("""
        SELECT s FROM Service s
        WHERE s.actif = true AND s.archivedAt IS NULL
        ORDER BY s.nomService
        """)
    List<Service> findActifsPourFiltre();
}
