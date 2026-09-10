package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.DemandeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DemandeServiceRepository extends JpaRepository<DemandeService, Integer> {

    Optional<DemandeService> findByReferenceDemande(String referenceDemande);

    boolean existsByReferenceDemande(String referenceDemande);

    long count();

    long countByActifTrue();

    long countByReferenceDemandeStartingWith(String prefix);

    @Query("SELECT COUNT(d) FROM DemandeService d WHERE d.dateDemande = CURRENT_DATE")
    long countDemandesAujourdhui();

    @Query("""
        SELECT d FROM DemandeService d
        WHERE (:recherche IS NULL OR LOWER(d.referenceDemande) LIKE LOWER(CONCAT('%', :recherche, '%')))
          AND (:societeId IS NULL OR d.societe.id = :societeId)
          AND (:serviceId IS NULL OR d.service.id = :serviceId)
          AND (:domaineId IS NULL OR d.societe.domaine.id = :domaineId)
          AND (:commercialId IS NULL OR d.commercial.id = :commercialId)
          AND (:dateDebut IS NULL OR d.debutService >= :dateDebut)
          AND (:dateFin IS NULL OR d.finService <= :dateFin)
          AND (:actif IS NULL OR d.actif = :actif)
          AND (:statut IS NULL OR EXISTS (
                SELECT 1 FROM HistoriqueDemande h
                WHERE h.demande = d
                  AND h.statut.statut = :statut
                  AND h.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d)
              ))
        """)
    Page<DemandeService> rechercher(@Param("recherche") String recherche,
                                    @Param("societeId") Integer societeId,
                                    @Param("serviceId") Integer serviceId,
                                    @Param("domaineId") Integer domaineId,
                                    @Param("commercialId") Integer commercialId,
                                    @Param("statut") String statut,
                                    @Param("dateDebut") LocalDate dateDebut,
                                    @Param("dateFin") LocalDate dateFin,
                                    @Param("actif") Boolean actif,
                                    Pageable pageable);

    List<DemandeService> findByDateDemandeGreaterThanEqualAndDateDemandeLessThanEqual(
            LocalDate dateDebut, LocalDate dateFin);

    @Query("""
        SELECT COALESCE(SUM(d.tarifServiceApplique), 0) FROM DemandeService d
        WHERE (:dateDebut IS NULL OR d.debutService >= :dateDebut)
          AND (:dateFin IS NULL OR d.finService <= :dateFin)
          AND (:societeId IS NULL OR d.societe.id = :societeId)
          AND (:serviceId IS NULL OR d.service.id = :serviceId)
          AND (:domaineId IS NULL OR d.societe.domaine.id = :domaineId)
          AND (:commercialId IS NULL OR d.commercial.id = :commercialId)
        """)
    BigDecimal sommeTarifTotal(@Param("dateDebut") LocalDate dateDebut,
                               @Param("dateFin") LocalDate dateFin,
                               @Param("societeId") Integer societeId,
                               @Param("serviceId") Integer serviceId,
                               @Param("domaineId") Integer domaineId,
                               @Param("commercialId") Integer commercialId);

    @Query("""
        SELECT h.statut.statut, COUNT(DISTINCT d.id)
        FROM DemandeService d JOIN d.historiques h
        WHERE h.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d)
        GROUP BY h.statut.statut
        """)
    List<Object[]> compterParStatut();

    @Query("""
        SELECT d.service.id, d.service.nomService, COUNT(DISTINCT d.id), COALESCE(SUM(d.tarifServiceApplique), 0)
        FROM DemandeService d
        GROUP BY d.service.id, d.service.nomService
        ORDER BY COALESCE(SUM(d.tarifServiceApplique), 0) DESC
        """)
    List<Object[]> revenuParService();

    @Query("""
        SELECT d.societe.id, d.societe.nomSociete, COUNT(DISTINCT d.id), COALESCE(SUM(d.tarifServiceApplique), 0)
        FROM DemandeService d
        GROUP BY d.societe.id, d.societe.nomSociete
        ORDER BY COUNT(DISTINCT d.id) DESC
        """)
    List<Object[]> demandesParSociete();

    @Query("""
        SELECT EXTRACT(YEAR FROM d.dateDemande), EXTRACT(MONTH FROM d.dateDemande), COUNT(d.id)
        FROM DemandeService d
        GROUP BY EXTRACT(YEAR FROM d.dateDemande), EXTRACT(MONTH FROM d.dateDemande)
        ORDER BY EXTRACT(YEAR FROM d.dateDemande), EXTRACT(MONTH FROM d.dateDemande)
        """)
    List<Object[]> compterParMois();

    @Query("""
        SELECT d FROM DemandeService d
        WHERE LOWER(d.referenceDemande) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(d.societe.nomSociete) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(d.service.nomService) LIKE LOWER(CONCAT('%', :q, '%'))
           OR (d.commercial IS NOT NULL AND LOWER(d.commercial.nomCommercial) LIKE LOWER(CONCAT('%', :q, '%')))
           OR LOWER(COALESCE(d.commentaire, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY d.dateDemande DESC
        """)
    List<DemandeService> rechercherGlobale(@Param("q") String q, Pageable pageable);

    @Query("""
        SELECT d.commercial.id, d.commercial.nomCommercial, COUNT(DISTINCT d.id)
        FROM DemandeService d
        WHERE d.commercial IS NOT NULL
        GROUP BY d.commercial.id, d.commercial.nomCommercial
        ORDER BY COUNT(DISTINCT d.id) DESC
        """)
    List<Object[]> compterParCommercial();

    @Query("""
        SELECT d.service.id, d.service.nomService, COUNT(DISTINCT d.id)
        FROM DemandeService d
        GROUP BY d.service.id, d.service.nomService
        ORDER BY COUNT(DISTINCT d.id) DESC
        """)
    List<Object[]> compterParService();
}
