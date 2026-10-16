package com.cabex.backend.client.repository;

import com.cabex.backend.client.entity.ClientResponsable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientResponsableRepository extends JpaRepository<ClientResponsable, Integer> {

    List<ClientResponsable> findBySocieteId(Integer societeId);

    List<ClientResponsable> findBySocieteIdAndActifTrue(Integer societeId);

    /** Responsables de plusieurs sociétés en une requête (colonne liste clients). */
    List<ClientResponsable> findBySocieteIdInOrderByIdAsc(java.util.Collection<Integer> societeIds);

    long countBySocieteId(Integer societeId);

    @Query("""
            SELECT r FROM ClientResponsable r
            LEFT JOIN r.societe s
            WHERE (:societeId IS NULL OR s.id = :societeId)
              AND (:actif IS NULL OR r.actif = :actif)
              AND (:recherche IS NULL OR :recherche = ''
                   OR LOWER(r.nomClient) LIKE LOWER(CONCAT('%', :recherche, '%'))
                   OR LOWER(COALESCE(r.contact, '')) LIKE LOWER(CONCAT('%', :recherche, '%'))
                   OR LOWER(COALESCE(r.email, '')) LIKE LOWER(CONCAT('%', :recherche, '%')))
            """)
    Page<ClientResponsable> rechercher(@Param("recherche") String recherche,
                                       @Param("societeId") Integer societeId,
                                       @Param("actif") Boolean actif,
                                       Pageable pageable);
}
