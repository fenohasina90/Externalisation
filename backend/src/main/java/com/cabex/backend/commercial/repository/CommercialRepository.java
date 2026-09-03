package com.cabex.backend.commercial.repository;

import com.cabex.backend.commercial.entity.Commercial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommercialRepository extends JpaRepository<Commercial, Integer> {

    Optional<Commercial> findByImmatricule(String immatricule);

    boolean existsByImmatricule(String immatricule);

    boolean existsByEmail(String email);

    @Query("""
            SELECT c FROM Commercial c
            WHERE (:recherche IS NULL OR :recherche = ''
                   OR LOWER(c.nomCommercial) LIKE LOWER(CONCAT('%', :recherche, '%'))
                   OR LOWER(c.immatricule) LIKE LOWER(CONCAT('%', :recherche, '%')))
              AND (:actif IS NULL OR c.actif = :actif)
            """)
    Page<Commercial> rechercher(@Param("recherche") String recherche,
                                @Param("actif") Boolean actif,
                                Pageable pageable);
}
