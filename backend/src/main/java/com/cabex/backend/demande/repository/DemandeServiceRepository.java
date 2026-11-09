package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.DemandeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /** Nombre de demandes créées dans l'année (références du nouveau format DEM-*-*-AAAA-EXT-*). */
    @Query("SELECT COUNT(d) FROM DemandeService d WHERE d.referenceDemande LIKE CONCAT('%', :annee, 'EXT%')")
    long countByAnnee(@Param("annee") Integer annee);
    

    @Query("SELECT COUNT(d) FROM DemandeService d WHERE d.dateDemande = CURRENT_DATE")
    long countDemandesAujourdhui();

    /**
     * Filtre de la liste des demandes : la recherche porte sur la référence de
     * la demande et sur le nom du client. La jointure sur la société est
     * externe pour ne pas perdre les demandes de création d'entreprise
     * (`societe_id` NULL).
     *
     * <p>Filtre `situation` — situation de règlement déduite des échéances,
     * dans l'ordre de précédence de {@code LigneServiceService} :
     * <ul>
     *   <li>{@code PAYE} : au moins une échéance et toutes soldées ;</li>
     *   <li>{@code EN_RETARD} : au moins une échéance dépassée avec un reste ;</li>
     *   <li>{@code PARTIEL} : reste dû, aucune échéance en retard, mais un
     *       paiement déjà enregistré ;</li>
     *   <li>{@code EN_ATTENTE} : reste dû, aucune échéance en retard, aucun
     *       paiement ;</li>
     *   <li>{@code NON_PLANIFIE} : aucun plan de paiement établi.</li>
     * </ul>
     * Les prédicats sont mutuellement exclusifs par construction.
     */
    @EntityGraph(attributePaths = {"societe", "commercial", "clientResponsable"})
    @Query("""
        SELECT d FROM DemandeService d
        LEFT JOIN d.societe s LEFT JOIN d.commercial c
        WHERE (:recherche IS NULL OR (
                LOWER(d.referenceDemande) LIKE LOWER(CONCAT('%', :recherche, '%'))
             OR (s IS NOT NULL AND LOWER(s.nomSociete) LIKE LOWER(CONCAT('%', :recherche, '%')))
          ))
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.service.id = :serviceId))
          AND (:commercialId IS NULL OR c.id = :commercialId)
          AND (:dateDebut IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.debutService >= :dateDebut))
          AND (:dateFin IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.finService <= :dateFin))
          AND (:actif IS NULL OR d.actif = :actif)
          AND (:statut IS NULL OR EXISTS (
                SELECT 1 FROM HistoriqueDemande h
                WHERE h.demande = d
                  AND h.statut.statut = :statut
                  AND h.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d)
              ))
          AND (
            :situation IS NULL
            OR (:situation = 'PAYE'
                AND NOT EXISTS (SELECT 1 FROM EcheancePaiement e
                                WHERE e.proposPaiement.demande = d
                                  AND e.montantPaye < e.montantAttendu)
                AND EXISTS (SELECT 1 FROM EcheancePaiement e
                            WHERE e.proposPaiement.demande = d))
            OR (:situation = 'EN_RETARD'
                AND EXISTS (SELECT 1 FROM EcheancePaiement e
                            WHERE e.proposPaiement.demande = d
                              AND e.dateEcheance < :aujourdhui
                              AND e.montantPaye < e.montantAttendu))
            OR (:situation = 'PARTIEL'
                AND EXISTS (SELECT 1 FROM EcheancePaiement e
                            WHERE e.proposPaiement.demande = d
                              AND e.montantPaye < e.montantAttendu)
                AND NOT EXISTS (SELECT 1 FROM EcheancePaiement e
                                WHERE e.proposPaiement.demande = d
                                  AND e.dateEcheance < :aujourdhui
                                  AND e.montantPaye < e.montantAttendu)
                AND EXISTS (SELECT 1 FROM EcheancePaiement e
                            WHERE e.proposPaiement.demande = d AND e.montantPaye > 0))
            OR (:situation = 'EN_ATTENTE'
                AND EXISTS (SELECT 1 FROM ProposPaiement p WHERE p.demande = d)
                AND (
                  NOT EXISTS (SELECT 1 FROM EcheancePaiement e
                              WHERE e.proposPaiement.demande = d)
                  OR (
                    EXISTS (SELECT 1 FROM EcheancePaiement e
                            WHERE e.proposPaiement.demande = d
                              AND e.montantPaye < e.montantAttendu)
                    AND NOT EXISTS (SELECT 1 FROM EcheancePaiement e
                                    WHERE e.proposPaiement.demande = d
                                      AND e.dateEcheance < :aujourdhui
                                      AND e.montantPaye < e.montantAttendu)
                    AND NOT EXISTS (SELECT 1 FROM EcheancePaiement e
                                    WHERE e.proposPaiement.demande = d AND e.montantPaye > 0)
                  )
                ))
            OR (:situation = 'NON_PLANIFIE'
                AND NOT EXISTS (SELECT 1 FROM ProposPaiement p WHERE p.demande = d))
          )
        """)
    Page<DemandeService> rechercher(@Param("recherche") String recherche,
                                    @Param("societeId") Integer societeId,
                                    @Param("serviceId") Integer serviceId,
                                    @Param("commercialId") Integer commercialId,
                                    @Param("statut") String statut,
                                    @Param("dateDebut") LocalDate dateDebut,
                                    @Param("dateFin") LocalDate dateFin,
                                    @Param("actif") Boolean actif,
                                    @Param("situation") String situation,
                                    @Param("aujourdhui") LocalDate aujourdhui,
                                    Pageable pageable);

    /**
     * Situation de règlement et nombre d'échéances pour un lot de demandes,
     * en une seule requête (évite un aller-retour par ligne de liste).
     * Colonnes : id, nb plans, nb échéances, nb avec reste, nb en retard,
     * nb payées — agrégats interpreétés par {@code SituationPaiement.classer}.
     */
    @Query("""
        SELECT d.id,
               COUNT(DISTINCT p.id),
               COUNT(e.id),
               SUM(CASE WHEN e.montantPaye < e.montantAttendu THEN 1 ELSE 0 END),
               SUM(CASE WHEN e.dateEcheance < :aujourdhui AND e.montantPaye < e.montantAttendu
                        THEN 1 ELSE 0 END),
               SUM(CASE WHEN e.montantPaye > 0 THEN 1 ELSE 0 END)
        FROM DemandeService d
        LEFT JOIN LigneService l ON l.demande = d
        LEFT JOIN ProposPaiement p ON p.ligneService = l
        LEFT JOIN EcheancePaiement e ON e.proposPaiement = p
        WHERE d.id IN :ids
        GROUP BY d.id
        """)
    List<Object[]> situations(@Param("ids") List<Integer> ids, @Param("aujourdhui") LocalDate aujourdhui);

    List<DemandeService> findByDateDemandeGreaterThanEqualAndDateDemandeLessThanEqual(
            LocalDate dateDebut, LocalDate dateFin);

    @Query("""
        SELECT COALESCE(SUM(l.tarif), 0) FROM DemandeService d JOIN d.lignes l
        LEFT JOIN d.societe s LEFT JOIN d.commercial c
        WHERE (:dateDebut IS NULL OR l.debutService >= :dateDebut)
          AND (:dateFin IS NULL OR l.finService <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR l.service.id = :serviceId)
          AND (:commercialId IS NULL OR c.id = :commercialId)
        """)
    BigDecimal sommeTarifTotal(@Param("dateDebut") LocalDate dateDebut,
                               @Param("dateFin") LocalDate dateFin,
                               @Param("societeId") Integer societeId,
                               @Param("serviceId") Integer serviceId,
                               @Param("commercialId") Integer commercialId);

    @Query("""
        SELECT h.statut.statut, COUNT(DISTINCT d.id)
        FROM DemandeService d JOIN d.historiques h
        WHERE h.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d)
        GROUP BY h.statut.statut
        """)
    List<Object[]> compterParStatut();

    @Query("""
        SELECT l.service.id, l.service.nomService, COUNT(DISTINCT l.demande.id), COALESCE(SUM(l.tarif), 0)
        FROM LigneService l
        GROUP BY l.service.id, l.service.nomService
        ORDER BY COALESCE(SUM(l.tarif), 0) DESC
        """)
    List<Object[]> revenuParService();

    @Query("""
        SELECT s.id, s.nomSociete, COUNT(DISTINCT d.id), COALESCE(SUM(l.tarif), 0)
        FROM DemandeService d LEFT JOIN d.lignes l
        LEFT JOIN d.societe s
        GROUP BY s.id, s.nomSociete
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
        LEFT JOIN d.societe s LEFT JOIN d.commercial c
        WHERE LOWER(d.referenceDemande) LIKE LOWER(CONCAT('%', :q, '%'))
           OR (s IS NOT NULL AND LOWER(s.nomSociete) LIKE LOWER(CONCAT('%', :q, '%')))
           OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND LOWER(l.service.nomService) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (c IS NOT NULL AND LOWER(c.nomCommercial) LIKE LOWER(CONCAT('%', :q, '%')))
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
        SELECT l.service.id, l.service.nomService, COUNT(DISTINCT l.demande.id)
        FROM LigneService l
        GROUP BY l.service.id, l.service.nomService
        ORDER BY COUNT(DISTINCT l.demande.id) DESC
        """)
    List<Object[]> compterParService();

    // ------------------------------------------------------------------
    // Tableau de bord filtré : période, société cliente et service.
    // ------------------------------------------------------------------

    @Query("""
        SELECT COUNT(d) FROM DemandeService d
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.service.id = :serviceId))
        """)
    long countFiltre(@Param("dateDebut") LocalDate dateDebut,
                     @Param("dateFin") LocalDate dateFin,
                     @Param("societeId") Integer societeId,
                     @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT COUNT(DISTINCT s.id) FROM DemandeService d
        JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.service.id = :serviceId))
        """)
    long compterClientsFiltre(@Param("dateDebut") LocalDate dateDebut,
                              @Param("dateFin") LocalDate dateFin,
                              @Param("societeId") Integer societeId,
                              @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT h.statut.statut, COUNT(DISTINCT d.id)
        FROM DemandeService d JOIN d.historiques h
        LEFT JOIN d.societe s
        WHERE h.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d)
          AND d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.service.id = :serviceId))
        GROUP BY h.statut.statut
        """)
    List<Object[]> compterParStatutFiltre(@Param("dateDebut") LocalDate dateDebut,
                                          @Param("dateFin") LocalDate dateFin,
                                          @Param("societeId") Integer societeId,
                                          @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT EXTRACT(YEAR FROM d.dateDemande), EXTRACT(MONTH FROM d.dateDemande), COUNT(DISTINCT d.id)
        FROM DemandeService d
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.service.id = :serviceId))
        GROUP BY EXTRACT(YEAR FROM d.dateDemande), EXTRACT(MONTH FROM d.dateDemande)
        ORDER BY EXTRACT(YEAR FROM d.dateDemande), EXTRACT(MONTH FROM d.dateDemande)
        """)
    List<Object[]> compterParMoisFiltre(@Param("dateDebut") LocalDate dateDebut,
                                        @Param("dateFin") LocalDate dateFin,
                                        @Param("societeId") Integer societeId,
                                        @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT l.service.id, l.service.nomService, COUNT(DISTINCT l.demande.id)
        FROM LigneService l JOIN l.demande d
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR l.service.id = :serviceId)
        GROUP BY l.service.id, l.service.nomService
        ORDER BY COUNT(DISTINCT l.demande.id) DESC
        """)
    List<Object[]> compterParServiceFiltre(@Param("dateDebut") LocalDate dateDebut,
                                           @Param("dateFin") LocalDate dateFin,
                                           @Param("societeId") Integer societeId,
                                           @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT l.service.id, l.service.nomService, COUNT(DISTINCT l.demande.id), COALESCE(SUM(l.tarif), 0)
        FROM LigneService l JOIN l.demande d
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR l.service.id = :serviceId)
        GROUP BY l.service.id, l.service.nomService
        ORDER BY COALESCE(SUM(l.tarif), 0) DESC
        """)
    List<Object[]> revenuParServiceFiltre(@Param("dateDebut") LocalDate dateDebut,
                                          @Param("dateFin") LocalDate dateFin,
                                          @Param("societeId") Integer societeId,
                                          @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT s.id, s.nomSociete, COUNT(DISTINCT d.id), COALESCE(SUM(l.tarif), 0)
        FROM DemandeService d
        LEFT JOIN d.lignes l ON (:serviceId IS NULL OR l.service.id = :serviceId)
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR l.service.id = :serviceId)
        GROUP BY s.id, s.nomSociete
        ORDER BY COUNT(DISTINCT d.id) DESC
        """)
    List<Object[]> demandesParSocieteFiltre(@Param("dateDebut") LocalDate dateDebut,
                                            @Param("dateFin") LocalDate dateFin,
                                            @Param("societeId") Integer societeId,
                                            @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT d.commercial.id, d.commercial.nomCommercial, COUNT(DISTINCT d.id)
        FROM DemandeService d
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND d.commercial IS NOT NULL
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.service.id = :serviceId))
        GROUP BY d.commercial.id, d.commercial.nomCommercial
        ORDER BY COUNT(DISTINCT d.id) DESC
        """)
    List<Object[]> compterParCommercialFiltre(@Param("dateDebut") LocalDate dateDebut,
                                              @Param("dateFin") LocalDate dateFin,
                                              @Param("societeId") Integer societeId,
                                              @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT COALESCE(SUM(l.tarif), 0)
        FROM DemandeService d JOIN d.lignes l
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR l.service.id = :serviceId)
        """)
    BigDecimal sommeTarifDashboard(@Param("dateDebut") LocalDate dateDebut,
                                   @Param("dateFin") LocalDate dateFin,
                                   @Param("societeId") Integer societeId,
                                   @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT COUNT(DISTINCT l.service.id)
        FROM LigneService l JOIN l.demande d
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR l.service.id = :serviceId)
        """)
    long compterServicesFiltre(@Param("dateDebut") LocalDate dateDebut,
                               @Param("dateFin") LocalDate dateFin,
                               @Param("societeId") Integer societeId,
                               @Param("serviceId") Integer serviceId);
}
