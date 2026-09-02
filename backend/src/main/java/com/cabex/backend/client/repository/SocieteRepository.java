package com.cabex.backend.client.repository;

import com.cabex.backend.client.entity.Societe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocieteRepository extends JpaRepository<Societe, Integer> {

    boolean existsByNomSociete(String nomSociete);

    boolean existsByEmail(String email);

    long countByActifTrue();

    @Query("""
        SELECT s FROM Societe s
        WHERE (:recherche IS NULL
               OR LOWER(s.nomSociete) LIKE LOWER(CONCAT('%', :recherche, '%'))
               OR LOWER(COALESCE(s.activite, '')) LIKE LOWER(CONCAT('%', :recherche, '%'))
               OR LOWER(COALESCE(s.contact, '')) LIKE LOWER(CONCAT('%', :recherche, '%')))
          AND (:actif IS NULL OR s.actif = :actif)
        """)
    Page<Societe> rechercher(@Param("recherche") String recherche,
                             @Param("actif") Boolean actif,
                             Pageable pageable);
}
