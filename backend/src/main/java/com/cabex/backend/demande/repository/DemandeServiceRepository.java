package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.DemandeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DemandeServiceRepository extends JpaRepository<DemandeService, Integer> {

    Optional<DemandeService> findByReferenceDemande(String referenceDemande);

    boolean existsByReferenceDemande(String referenceDemande);

    long count();

    long countByActifTrue();

    @Query("SELECT COUNT(d) FROM DemandeService d WHERE d.dateDemande = CURRENT_DATE")
    long countDemandesAujourdhui();

    @Query("""
        SELECT d FROM DemandeService d
        WHERE (:recherche IS NULL OR LOWER(d.referenceDemande) LIKE LOWER(CONCAT('%', :recherche, '%')))
          AND (:societeId IS NULL OR d.societe.id = :societeId)
          AND (:serviceId IS NULL OR d.service.id = :serviceId)
          AND (:commercialId IS NULL OR d.commercial.id = :commercialId)
          AND (:dateDebut IS NULL OR d.debutService >= :dateDebut)
          AND (:dateFin IS NULL OR d.finService <= :dateFin)
          AND (:actif IS NULL OR d.actif = :actif)
        """)
    Page<DemandeService> rechercher(@Param("recherche") String recherche,
                                    @Param("societeId") Integer societeId,
                                    @Param("serviceId") Integer serviceId,
                                    @Param("commercialId") Integer commercialId,
                                    @Param("dateDebut") LocalDate dateDebut,
                                    @Param("dateFin") LocalDate dateFin,
                                    @Param("actif") Boolean actif,
                                    Pageable pageable);
}
