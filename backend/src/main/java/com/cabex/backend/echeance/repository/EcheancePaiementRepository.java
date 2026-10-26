package com.cabex.backend.echeance.repository;

import com.cabex.backend.echeance.entity.EcheancePaiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EcheancePaiementRepository extends JpaRepository<EcheancePaiement, Integer> {

    List<EcheancePaiement> findByProposPaiementIdOrderByNumeroEcheanceAsc(Integer proposPaiementId);

    /** Échéances d'un plan, de la plus récente à la plus ancienne. */
    List<EcheancePaiement> findByProposPaiementIdOrderByDateEcheanceDescNumeroEcheanceDesc(
            Integer proposPaiementId);

    boolean existsByProposPaiementIdAndNumeroEcheance(Integer proposPaiementId, Integer numeroEcheance);

    long countByProposPaiementId(Integer proposPaiementId);

    @Query("SELECT SUM(e.montantAttendu) FROM EcheancePaiement e WHERE e.proposPaiement.id = :proposId")
    BigDecimal sommeMontantAttendu(@Param("proposId") Integer proposId);

    List<EcheancePaiement> findByDateEcheanceBetweenAndMontantPayeLessThan(LocalDate debut, LocalDate fin,
                                                                           BigDecimal montantPaye);

    @Query("""
            SELECT e FROM EcheancePaiement e
            WHERE e.proposPaiement.demande.societe.id = :societeId
            ORDER BY e.dateEcheance DESC, e.numeroEcheance DESC
            """)
    List<EcheancePaiement> findBySocieteId(@Param("societeId") Integer societeId);

    @Query("""
            SELECT e FROM EcheancePaiement e
            JOIN FETCH e.proposPaiement p
            JOIN FETCH p.demande d
            LEFT JOIN FETCH d.societe s
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
            ORDER BY e.dateEcheance DESC, e.numeroEcheance DESC
            """)
    List<EcheancePaiement> findEnRetard(@Param("jour") LocalDate jour);

    @Query("""
            SELECT COALESCE(SUM(e.montantAttendu - e.montantPaye), 0) FROM EcheancePaiement e
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
            """)
    BigDecimal sommeEnRetard(@Param("jour") LocalDate jour);

    @Query("""
            SELECT COALESCE(SUM(e.montantAttendu - e.montantPaye), 0) FROM EcheancePaiement e
            LEFT JOIN e.proposPaiement.demande.societe s LEFT JOIN s.domaine dom
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
              AND (:societeId IS NULL OR s.id = :societeId)
              AND (:serviceId IS NULL OR EXISTS (
                    SELECT 1 FROM LigneService l
                    WHERE l.demande = e.proposPaiement.demande AND l.service.id = :serviceId))
              AND (:domaineId IS NULL OR dom.id = :domaineId)
            """)
    BigDecimal sommeEnRetardFiltree(@Param("jour") LocalDate jour,
                                    @Param("societeId") Integer societeId,
                                    @Param("serviceId") Integer serviceId,
                                    @Param("domaineId") Integer domaineId);

    @Query("SELECT COUNT(e) FROM EcheancePaiement e WHERE e.dateEcheance < :jour AND e.montantPaye < e.montantAttendu")
    long countEnRetard(@Param("jour") LocalDate jour);

    @Query("""
            SELECT COUNT(e) FROM EcheancePaiement e
            WHERE e.dateEcheance >= :jour
              AND e.dateEcheance <= :fin
              AND e.montantPaye < e.montantAttendu
            """)
    long countProches(@Param("jour") LocalDate jour, @Param("fin") LocalDate fin);

    @Query("""
            SELECT e FROM EcheancePaiement e
            JOIN FETCH e.proposPaiement p
            JOIN FETCH p.demande d
            LEFT JOIN FETCH d.societe s
            WHERE e.dateEcheance >= :jour
              AND e.dateEcheance <= :limite
              AND e.montantPaye < e.montantAttendu
            ORDER BY e.dateEcheance DESC, e.numeroEcheance DESC
            """)
    List<EcheancePaiement> findAVenir(@Param("jour") LocalDate jour,
                                      @Param("limite") LocalDate limite);

    @Query(value = """
            SELECT e FROM EcheancePaiement e
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
            ORDER BY e.dateEcheance DESC, e.numeroEcheance DESC
            """,
            countQuery = """
            SELECT COUNT(e) FROM EcheancePaiement e
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
            """)
    Page<EcheancePaiement> findEnRetardPagine(@Param("jour") LocalDate jour, Pageable pageable);

    @Query(value = """
            SELECT e FROM EcheancePaiement e
            WHERE e.dateEcheance >= :jour
              AND e.montantPaye < e.montantAttendu
            ORDER BY e.dateEcheance DESC, e.numeroEcheance DESC
            """,
            countQuery = """
            SELECT COUNT(e) FROM EcheancePaiement e
            WHERE e.dateEcheance >= :jour
              AND e.montantPaye < e.montantAttendu
            """)
    Page<EcheancePaiement> findAVenirPagine(@Param("jour") LocalDate jour, Pageable pageable);

    // ------------------------------------------------------------------
    // Tableau de bord filtré : période, société cliente et service.
    // ------------------------------------------------------------------

    @Query("""
            SELECT COUNT(e) FROM EcheancePaiement e
            LEFT JOIN e.proposPaiement.demande.societe s
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
              AND (:dateDebut IS NULL OR e.dateEcheance >= :dateDebut)
              AND (:dateFin IS NULL OR e.dateEcheance <= :dateFin)
              AND (:societeId IS NULL OR s.id = :societeId)
              AND (:serviceId IS NULL OR EXISTS (
                    SELECT 1 FROM LigneService l
                    WHERE l.demande = e.proposPaiement.demande AND l.service.id = :serviceId))
            """)
    long countEnRetardFiltre(@Param("jour") LocalDate jour,
                             @Param("dateDebut") LocalDate dateDebut,
                             @Param("dateFin") LocalDate dateFin,
                             @Param("societeId") Integer societeId,
                             @Param("serviceId") Integer serviceId);

    @Query("""
            SELECT COUNT(e) FROM EcheancePaiement e
            LEFT JOIN e.proposPaiement.demande.societe s
            WHERE e.dateEcheance >= :jour
              AND e.dateEcheance <= :fin
              AND e.montantPaye < e.montantAttendu
              AND (:dateDebut IS NULL OR e.dateEcheance >= :dateDebut)
              AND (:dateFin IS NULL OR e.dateEcheance <= :dateFin)
              AND (:societeId IS NULL OR s.id = :societeId)
              AND (:serviceId IS NULL OR EXISTS (
                    SELECT 1 FROM LigneService l
                    WHERE l.demande = e.proposPaiement.demande AND l.service.id = :serviceId))
            """)
    long countProchesFiltre(@Param("jour") LocalDate jour,
                            @Param("fin") LocalDate fin,
                            @Param("dateDebut") LocalDate dateDebut,
                            @Param("dateFin") LocalDate dateFin,
                            @Param("societeId") Integer societeId,
                            @Param("serviceId") Integer serviceId);

    @Query("""
            SELECT e FROM EcheancePaiement e
            JOIN FETCH e.proposPaiement p
            JOIN FETCH p.demande d
            LEFT JOIN FETCH d.societe s
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
              AND (:dateDebut IS NULL OR e.dateEcheance >= :dateDebut)
              AND (:dateFin IS NULL OR e.dateEcheance <= :dateFin)
              AND (:societeId IS NULL OR s.id = :societeId)
              AND (:serviceId IS NULL OR EXISTS (
                    SELECT 1 FROM LigneService l
                    WHERE l.demande = e.proposPaiement.demande AND l.service.id = :serviceId))
            ORDER BY e.dateEcheance DESC, e.numeroEcheance DESC
            """)
    List<EcheancePaiement> findEnRetardFiltre(@Param("jour") LocalDate jour,
                                              @Param("dateDebut") LocalDate dateDebut,
                                              @Param("dateFin") LocalDate dateFin,
                                              @Param("societeId") Integer societeId,
                                              @Param("serviceId") Integer serviceId);

    @Query("""
            SELECT e FROM EcheancePaiement e
            JOIN FETCH e.proposPaiement p
            JOIN FETCH p.demande d
            LEFT JOIN FETCH d.societe s
            WHERE e.dateEcheance >= :jour
              AND e.dateEcheance <= :limite
              AND e.montantPaye < e.montantAttendu
              AND (:dateDebut IS NULL OR e.dateEcheance >= :dateDebut)
              AND (:dateFin IS NULL OR e.dateEcheance <= :dateFin)
              AND (:societeId IS NULL OR s.id = :societeId)
              AND (:serviceId IS NULL OR EXISTS (
                    SELECT 1 FROM LigneService l
                    WHERE l.demande = e.proposPaiement.demande AND l.service.id = :serviceId))
            ORDER BY e.dateEcheance DESC, e.numeroEcheance DESC
            """)
    List<EcheancePaiement> findAVenirFiltre(@Param("jour") LocalDate jour,
                                            @Param("limite") LocalDate limite,
                                            @Param("dateDebut") LocalDate dateDebut,
                                            @Param("dateFin") LocalDate dateFin,
                                            @Param("societeId") Integer societeId,
                                            @Param("serviceId") Integer serviceId);
}
