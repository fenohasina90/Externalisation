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

    @Query("SELECT d FROM DemandeService d WHERE d.societe.id = :societeId AND d.actif = true ORDER BY d.id ASC")
    List<DemandeService> findBySocieteIdAndActifTrueOrderByIdAsc(@Param("societeId") Integer societeId);

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
     * <p>Filtre `situation` — situation de règlement :
     * <ul>
     *   <li>{@code PAYE} : tout est réglé (échéances soldées ou factures validées
     *       soldées) ET la demande est {@code TERMINEE} ;</li>
     *   <li>{@code EN_RETARD} : une échéance est dépassée avec un reste, ou une
     *       facture validée non mensuelle date de plus de {@code deadlinePrestationDebours},
     *       ou une facture mensuelle de plus de {@code deadlineAbonnement}, avec
     *       un règlement partiel ou absent ;</li>
     *   <li>{@code PARTIEL} : reste dû et au moins un paiement enregistré,
     *       ou tout réglé mais demande qui n'est pas encore {@code TERMINEE} ;</li>
     *   <li>{@code EN_ATTENTE} : aucun paiement enregistré et reste dû ou rien à
     *       régler pour l'instant ;</li>
     *   <li>{@code NON_PLANIFIE} : aucun plan de paiement établi, aucune facture
     *       validée et aucun paiement.</li>
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
             OR (c IS NOT NULL AND LOWER(c.nomCommercial) LIKE LOWER(CONCAT('%', :recherche, '%')))
             OR EXISTS (SELECT 1 FROM LigneService l
                          WHERE l.demande = d
                            AND LOWER(l.service.nomService) LIKE LOWER(CONCAT('%', :recherche, '%')))
             OR EXISTS (SELECT 1 FROM Facture f
                          WHERE f.demande = d
                            AND LOWER(f.referenceFacture) LIKE LOWER(CONCAT('%', :recherche, '%')))
             OR EXISTS (SELECT 1 FROM FactureValidee v
                          WHERE v.demande = d
                            AND LOWER(v.referenceFactureValidee) LIKE LOWER(CONCAT('%', :recherche, '%')))
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
          AND (:categorie IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.categorie = :categorie))
          AND (:typePrestation IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = d AND l.typePrestation = :typePrestation))
          AND (:situation IS NULL
            OR (:situation = 'PAYE'
                AND EXISTS (SELECT 1 FROM HistoriqueDemande hT
                            WHERE hT.demande = d
                              AND hT.statut.statut = 'TERMINEE'
                              AND hT.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d))
                AND NOT EXISTS (SELECT 1 FROM FactureValidee fv
                                WHERE fv.demande = d
                                  AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                       FROM Paiement p
                                       WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant))
            OR (:situation = 'EN_RETARD'
                AND (
                  EXISTS (SELECT 1 FROM FactureValidee fv
                            WHERE fv.demande = d
                              AND fv.mois IS NULL
                              AND fv.valideLe <= :deadlinePrestationDebours
                              AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                   FROM Paiement p
                                   WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant)
                  OR EXISTS (SELECT 1 FROM FactureValidee fv
                            WHERE fv.demande = d
                              AND fv.mois IS NOT NULL
                              AND fv.valideLe <= :deadlineAbonnement
                              AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                   FROM Paiement p
                                   WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant)
                ))
            OR (:situation = 'PARTIEL'
                AND NOT (
                  EXISTS (SELECT 1 FROM HistoriqueDemande hT
                            WHERE hT.demande = d
                              AND hT.statut.statut = 'TERMINEE'
                              AND hT.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d))
                  AND NOT EXISTS (SELECT 1 FROM FactureValidee fv
                                  WHERE fv.demande = d
                                    AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                         FROM Paiement p
                                         WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant))
                AND NOT (
                  EXISTS (SELECT 1 FROM FactureValidee fv
                            WHERE fv.demande = d
                              AND fv.mois IS NULL
                              AND fv.valideLe <= :deadlinePrestationDebours
                              AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                   FROM Paiement p
                                   WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant)
                  OR EXISTS (SELECT 1 FROM FactureValidee fv
                            WHERE fv.demande = d
                              AND fv.mois IS NOT NULL
                              AND fv.valideLe <= :deadlineAbonnement
                              AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                   FROM Paiement p
                                   WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant))
                AND (
                  EXISTS (SELECT 1 FROM Paiement pA
                            WHERE pA.demande = d AND pA.actif = true)
                  OR EXISTS (SELECT 1 FROM LigneService l
                               WHERE l.demande = d AND l.statut = 'PARTIEL')))
            OR (:situation = 'EN_ATTENTE'
                AND NOT (
                  EXISTS (SELECT 1 FROM HistoriqueDemande hT
                            WHERE hT.demande = d
                              AND hT.statut.statut = 'TERMINEE'
                              AND hT.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2 WHERE h2.demande = d))
                  AND NOT EXISTS (SELECT 1 FROM FactureValidee fv
                                  WHERE fv.demande = d
                                    AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                         FROM Paiement p
                                         WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant))
                AND NOT (
                  EXISTS (SELECT 1 FROM FactureValidee fv
                            WHERE fv.demande = d
                              AND fv.mois IS NULL
                              AND fv.valideLe <= :deadlinePrestationDebours
                              AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                   FROM Paiement p
                                   WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant)
                  OR EXISTS (SELECT 1 FROM FactureValidee fv
                            WHERE fv.demande = d
                              AND fv.mois IS NOT NULL
                              AND fv.valideLe <= :deadlineAbonnement
                              AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                   FROM Paiement p
                                   WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant))
                AND NOT (
                  EXISTS (SELECT 1 FROM Paiement pA
                            WHERE pA.demande = d AND pA.actif = true)
                  OR EXISTS (SELECT 1 FROM LigneService l
                               WHERE l.demande = d AND l.statut = 'PARTIEL'))
                AND (
                  EXISTS (SELECT 1 FROM FactureValidee fv
                            WHERE fv.demande = d
                              AND (SELECT COALESCE(SUM(p.montantPaye), 0)
                                   FROM Paiement p
                                   WHERE p.factureValidee.id = fv.id AND p.actif = true) < fv.montant)
                  OR EXISTS (SELECT 1 FROM LigneService l
                               WHERE l.demande = d
                                 AND l.statut IS NOT NULL
                                 AND l.statut NOT IN ('PAYEE','TERMINE','ANNULEE'))))
            OR (:situation = 'NON_PLANIFIE'
                AND NOT EXISTS (SELECT 1 FROM FactureValidee fv WHERE fv.demande = d)
                AND NOT EXISTS (SELECT 1 FROM Paiement p WHERE p.demande = d AND p.actif = true)
                AND NOT EXISTS (SELECT 1 FROM LigneService l WHERE l.demande = d))
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
                                    @Param("categorie") String categorie,
                                    @Param("typePrestation") String typePrestation,
                                    @Param("aujourdhui") LocalDate aujourdhui,
                                    @Param("deadlinePrestationDebours") java.time.LocalDateTime deadlinePrestationDebours,
                                    @Param("deadlineAbonnement") java.time.LocalDateTime deadlineAbonnement,
                                    Pageable pageable);

    /**
     * Repli « sans facture » : nombre de lignes par demande et estimation du
     * statut des lignes (TERMINE/PAYEE, PARTIEL) pour classer la situation
     * lorsque la demande n'a pas (encore) de facture validée.
     * Colonnes : id, nb lignes, nb lignes payées, nb lignes partielles.
     */
    @Query("""
        SELECT d.id,
               COUNT(DISTINCT l.id),
               COUNT(DISTINCT CASE WHEN l.statut IN ('TERMINE','PAYEE') THEN l.id END),
               COUNT(DISTINCT CASE WHEN l.statut = 'PARTIEL' THEN l.id END)
        FROM DemandeService d
        LEFT JOIN LigneService l ON l.demande = d
        WHERE d.id IN :ids
        GROUP BY d.id
        """)
    List<Object[]> situationsLignes(@Param("ids") List<Integer> ids);

    List<DemandeService> findByDateDemandeGreaterThanEqualAndDateDemandeLessThanEqual(
            LocalDate dateDebut, LocalDate dateFin);

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
           OR EXISTS (SELECT 1 FROM Facture f
                          WHERE f.demande = d
                            AND LOWER(f.referenceFacture) LIKE LOWER(CONCAT('%', :q, '%')))
           OR EXISTS (SELECT 1 FROM FactureValidee v
                          WHERE v.demande = d
                            AND LOWER(v.referenceFactureValidee) LIKE LOWER(CONCAT('%', :q, '%')))
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

    /**
     * Nombre de demandes ayant au moins une ligne de catégorie DEBOURS.
     * Les débours n'étant rattachés à aucun service, le filtre service est
     * volontairement ignoré ; la ligne est toujours présente côté tableau de bord.
     */
    @Query("""
        SELECT COUNT(DISTINCT d.id)
        FROM DemandeService d
        LEFT JOIN d.societe s
        WHERE d.actif = true
          AND EXISTS (SELECT 1 FROM LigneService l
                      WHERE l.demande = d AND l.categorie = 'DEBOURS')
          AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
        """)
    long compterDemandesAvecDeboursFiltre(@Param("dateDebut") LocalDate dateDebut,
                                          @Param("dateFin") LocalDate dateFin,
                                          @Param("societeId") Integer societeId);

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
