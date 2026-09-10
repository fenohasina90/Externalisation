package com.cabex.backend.service.repository;

import com.cabex.backend.service.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Integer> {

    boolean existsByNomService(String nomService);

    long countByActifTrue();

    @Query("""
        SELECT s FROM Service s
        WHERE (:recherche IS NULL
               OR LOWER(s.nomService) LIKE LOWER(CONCAT('%', :recherche, '%')))
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
}
