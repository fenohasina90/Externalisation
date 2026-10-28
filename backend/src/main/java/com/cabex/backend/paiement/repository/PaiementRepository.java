package com.cabex.backend.paiement.repository;

import com.cabex.backend.paiement.entity.Paiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Integer> {

    Optional<Paiement> findByIdAndActifTrue(Integer id);

    Optional<Paiement> findByReferencePaiementAndActifTrue(String referencePaiement);

    Optional<Paiement> findByReferencePaiement(String referencePaiement);

    boolean existsByReferencePaiement(String referencePaiement);

    long count();

    @Query("SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p WHERE p.actif = true")
    BigDecimal sommeTotalePayee();

    @Query("SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p WHERE p.demande.id = :demandeId AND p.actif = true")
    BigDecimal sommePayeeParDemande(@Param("demandeId") Integer demandeId);

    /** Montants payés par demande, en une seule requête pour toute une page. */
    @Query("""
        SELECT p.demande.id, COALESCE(SUM(p.montantPaye), 0)
        FROM Paiement p
        WHERE p.demande.id IN :demandeIds AND p.actif = true
        GROUP BY p.demande.id
        """)
    List<Object[]> sommesPayeesParDemandes(@Param("demandeIds") Collection<Integer> demandeIds);

    @Query("SELECT p FROM Paiement p WHERE p.demande.societe.id = :societeId AND p.actif = true ORDER BY p.datePaiement DESC")
    List<Paiement> findBySocieteId(@Param("societeId") Integer societeId);

    @Query("SELECT p FROM Paiement p WHERE p.echeance IS NOT NULL AND p.actif = true ORDER BY p.datePaiement DESC")
    List<Paiement> payesSurEcheance();

    @Query("SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p WHERE p.echeance.id = :echeanceId AND p.actif = true")
    BigDecimal sommePayeeParEcheance(@Param("echeanceId") Integer echeanceId);

    @Query("""
        SELECT p.demande.societe.id, COALESCE(SUM(p.montantPaye), 0)
        FROM Paiement p
        WHERE p.actif = true
        GROUP BY p.demande.societe.id
        """)
    List<Object[]> sommeParSociete();

    @Query("""
        SELECT COALESCE(COUNT(p), 0) FROM Paiement p
        WHERE p.actif = true AND p.echeance IS NOT NULL
        """)
    long compterPaiementsSurEcheance();

    @Query("""
        SELECT COALESCE(COUNT(p), 0) FROM Paiement p
        WHERE p.actif = true AND p.echeance IS NOT NULL
          AND p.datePaiement <= p.echeance.dateEcheance
        """)
    long compterPaiementsSurEcheanceATemps();

    /**
     * Filtre de l'historique des paiements : la recherche porte sur la
     * référence du paiement, celle de la demande, le client, le mode et la
     * référence de virement. La jointure sur la société est externe : une
     * demande de création d'entreprise n'a pas de société (`societe_id` NULL)
     * et ses paiements doivent rester trouvables.
     */
    @EntityGraph(attributePaths = {"demande", "demande.societe", "modePaiement", "createdBy"})
    @Query("""
        SELECT p FROM Paiement p
        LEFT JOIN p.demande.societe s
        WHERE p.actif = true
          AND (:recherche IS NULL OR (
                LOWER(p.referencePaiement) LIKE LOWER(CONCAT('%', :recherche, '%'))
             OR LOWER(p.demande.referenceDemande) LIKE LOWER(CONCAT('%', :recherche, '%'))
             OR (s IS NOT NULL AND LOWER(s.nomSociete) LIKE LOWER(CONCAT('%', :recherche, '%')))
             OR (p.modePaiement IS NOT NULL
                 AND LOWER(p.modePaiement.libelle) LIKE LOWER(CONCAT('%', :recherche, '%')))
             OR LOWER(COALESCE(p.referenceVirement, '')) LIKE LOWER(CONCAT('%', :recherche, '%'))
          ))
          AND (:demandeId IS NULL OR p.demande.id = :demandeId)
          AND (:modeId IS NULL OR p.modePaiement.id = :modeId)
          AND (:dateDebut IS NULL OR p.datePaiement >= :dateDebut)
          AND (:dateFin IS NULL OR p.datePaiement <= :dateFin)
        """)
    Page<Paiement> rechercher(@Param("recherche") String recherche,
                              @Param("demandeId") Integer demandeId,
                              @Param("modeId") Integer modeId,
                              @Param("dateDebut") LocalDate dateDebut,
                              @Param("dateFin") LocalDate dateFin,
                              Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p
        LEFT JOIN p.demande.societe s
        WHERE p.actif = true
          AND (:dateDebut IS NULL OR p.datePaiement >= :dateDebut)
          AND (:dateFin IS NULL OR p.datePaiement <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = p.demande AND l.service.id = :serviceId))
        """)
    BigDecimal sommePayee(@Param("dateDebut") LocalDate dateDebut,
                          @Param("dateFin") LocalDate dateFin,
                          @Param("societeId") Integer societeId,
                          @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT p.modePaiement.libelle, COALESCE(SUM(p.montantPaye), 0)
        FROM Paiement p
        LEFT JOIN p.demande.societe s
        WHERE p.actif = true
          AND (:dateDebut IS NULL OR p.datePaiement >= :dateDebut)
          AND (:dateFin IS NULL OR p.datePaiement <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = p.demande AND l.service.id = :serviceId))
        GROUP BY p.modePaiement.libelle
        """)
    List<Object[]> sommeParMode(@Param("dateDebut") LocalDate dateDebut,
                                @Param("dateFin") LocalDate dateFin,
                                @Param("societeId") Integer societeId,
                                @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT EXTRACT(YEAR FROM p.datePaiement), EXTRACT(MONTH FROM p.datePaiement), COALESCE(SUM(p.montantPaye), 0)
        FROM Paiement p
        WHERE p.actif = true
        GROUP BY EXTRACT(YEAR FROM p.datePaiement), EXTRACT(MONTH FROM p.datePaiement)
        ORDER BY EXTRACT(YEAR FROM p.datePaiement), EXTRACT(MONTH FROM p.datePaiement)
        """)
    List<Object[]> montantsParMois();

    @Query("""
        SELECT p FROM Paiement p
        LEFT JOIN p.demande.societe s LEFT JOIN p.demande.commercial c
        WHERE p.actif = true
          AND (:dateDebut IS NULL OR p.datePaiement >= :dateDebut)
          AND (:dateFin IS NULL OR p.datePaiement <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = p.demande AND l.service.id = :serviceId))
          AND (:commercialId IS NULL OR c.id = :commercialId)
        ORDER BY p.datePaiement DESC
        """)
    List<Paiement> rechercherRapport(@Param("dateDebut") LocalDate dateDebut,
                                     @Param("dateFin") LocalDate dateFin,
                                     @Param("societeId") Integer societeId,
                                     @Param("serviceId") Integer serviceId,
                                     @Param("commercialId") Integer commercialId);

    /**
     * Recherche globale : `actif = true` s'applique à toutes les branches
     * (règle R12 — les paiements archivés sont exclus de la recherche), et la
     * société est jointe en externe pour ne pas perdre les demandes de
     * création d'entreprise.
     */
    @Query("""
        SELECT p FROM Paiement p
        LEFT JOIN p.demande.societe s
        WHERE p.actif = true
          AND (
                LOWER(p.referencePaiement) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(p.demande.referenceDemande) LIKE LOWER(CONCAT('%', :q, '%'))
             OR (s IS NOT NULL AND LOWER(s.nomSociete) LIKE LOWER(CONCAT('%', :q, '%')))
             OR (p.modePaiement IS NOT NULL
                 AND LOWER(p.modePaiement.libelle) LIKE LOWER(CONCAT('%', :q, '%')))
          )
        ORDER BY p.datePaiement DESC
        """)
    List<Paiement> rechercherGlobale(@Param("q") String q, Pageable pageable);

    // ------------------------------------------------------------------
    // Tableau de bord filtré : période, société cliente et service.
    // ------------------------------------------------------------------

    @Query("""
        SELECT COUNT(p), COALESCE(SUM(CASE WHEN p.datePaiement <= p.echeance.dateEcheance THEN 1 ELSE 0 END), 0)
        FROM Paiement p
        LEFT JOIN p.demande.societe s
        WHERE p.actif = true
          AND p.echeance IS NOT NULL
          AND (:dateDebut IS NULL OR p.datePaiement >= :dateDebut)
          AND (:dateFin IS NULL OR p.datePaiement <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = p.demande AND l.service.id = :serviceId))
        """)
    List<Object[]> statistiquesPaiementsEcheance(@Param("dateDebut") LocalDate dateDebut,
                                                 @Param("dateFin") LocalDate dateFin,
                                                 @Param("societeId") Integer societeId,
                                                 @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT EXTRACT(YEAR FROM p.datePaiement), EXTRACT(MONTH FROM p.datePaiement), COALESCE(SUM(p.montantPaye), 0)
        FROM Paiement p
        LEFT JOIN p.demande.societe s
        WHERE p.actif = true
          AND (:dateDebut IS NULL OR p.datePaiement >= :dateDebut)
          AND (:dateFin IS NULL OR p.datePaiement <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = p.demande AND l.service.id = :serviceId))
        GROUP BY EXTRACT(YEAR FROM p.datePaiement), EXTRACT(MONTH FROM p.datePaiement)
        ORDER BY EXTRACT(YEAR FROM p.datePaiement), EXTRACT(MONTH FROM p.datePaiement)
        """)
    List<Object[]> montantsParMoisFiltre(@Param("dateDebut") LocalDate dateDebut,
                                         @Param("dateFin") LocalDate dateFin,
                                         @Param("societeId") Integer societeId,
                                         @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT s.id, COALESCE(SUM(p.montantPaye), 0)
        FROM Paiement p
        LEFT JOIN p.demande.societe s
        WHERE p.actif = true
          AND (:dateDebut IS NULL OR p.datePaiement >= :dateDebut)
          AND (:dateFin IS NULL OR p.datePaiement <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = p.demande AND l.service.id = :serviceId))
        GROUP BY s.id
        """)
    List<Object[]> sommeParSocieteFiltre(@Param("dateDebut") LocalDate dateDebut,
                                         @Param("dateFin") LocalDate dateFin,
                                         @Param("societeId") Integer societeId,
                                         @Param("serviceId") Integer serviceId);
}
