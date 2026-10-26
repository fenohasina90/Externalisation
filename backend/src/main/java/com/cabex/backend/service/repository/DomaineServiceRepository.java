package com.cabex.backend.service.repository;

import com.cabex.backend.service.entity.DomaineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DomaineServiceRepository extends JpaRepository<DomaineService, Integer> {

    boolean existsByNomDomaine(String nomDomaine);

    long countByActifTrue();

    @Query("""
        SELECT d FROM DomaineService d
        WHERE (:recherche IS NULL
               OR LOWER(d.nomDomaine) LIKE LOWER(CONCAT('%', :recherche, '%')))
          AND (:actif IS NULL OR d.actif = :actif)
        """)
    Page<DomaineService> rechercher(@Param("recherche") String recherche,
                                    @Param("actif") Boolean actif,
                                    Pageable pageable);

    @Query("""
        SELECT s.domaine.id, COUNT(s) FROM Societe s
        GROUP BY s.domaine.id
        """)
    List<Object[]> compterSocietesParDomaine();
}
