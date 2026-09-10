package com.cabex.backend.client.repository;

import com.cabex.backend.client.entity.Societe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
          AND (:domaineId IS NULL OR s.domaine.id = :domaineId)
        """)
    Page<Societe> rechercher(@Param("recherche") String recherche,
                             @Param("actif") Boolean actif,
                             @Param("domaineId") Integer domaineId,
                             Pageable pageable);

    @Query("""
        SELECT s FROM Societe s
        WHERE LOWER(s.nomSociete) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(COALESCE(s.contact, '')) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(COALESCE(s.activite, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY s.nomSociete
        """)
    List<Societe> rechercherGlobale(@Param("q") String q, Pageable pageable);

    @Query("""
        SELECT d.societe.id, COUNT(d) FROM DemandeService d
        GROUP BY d.societe.id
        """)
    List<Object[]> compterDemandesParSociete();
}
