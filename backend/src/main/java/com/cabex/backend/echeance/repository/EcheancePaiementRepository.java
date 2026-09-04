package com.cabex.backend.echeance.repository;

import com.cabex.backend.echeance.entity.EcheancePaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EcheancePaiementRepository extends JpaRepository<EcheancePaiement, Integer> {

    List<EcheancePaiement> findByProposPaiementIdOrderByNumeroEcheanceAsc(Integer proposPaiementId);

    boolean existsByProposPaiementIdAndNumeroEcheance(Integer proposPaiementId, Integer numeroEcheance);

    long countByProposPaiementId(Integer proposPaiementId);

    @Query("SELECT SUM(e.montantAttendu) FROM EcheancePaiement e WHERE e.proposPaiement.id = :proposId")
    BigDecimal sommeMontantAttendu(@Param("proposId") Integer proposId);

    List<EcheancePaiement> findByDateEcheanceBetweenAndMontantPayeLessThan(LocalDate debut, LocalDate fin,
                                                                           BigDecimal montantPaye);

    @Query("""
            SELECT e FROM EcheancePaiement e
            WHERE e.proposPaiement.demande.societe.id = :societeId
            ORDER BY e.dateEcheance ASC
            """)
    List<EcheancePaiement> findBySocieteId(@Param("societeId") Integer societeId);

    @Query("""
            SELECT e FROM EcheancePaiement e
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
            ORDER BY e.dateEcheance ASC
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
            WHERE e.dateEcheance < :jour
              AND e.montantPaye < e.montantAttendu
              AND (:societeId IS NULL OR e.proposPaiement.demande.societe.id = :societeId)
              AND (:serviceId IS NULL OR e.proposPaiement.demande.service.id = :serviceId)
              AND (:domaineId IS NULL OR e.proposPaiement.demande.service.domaine.id = :domaineId)
            """)
    BigDecimal sommeEnRetardFiltree(@Param("jour") LocalDate jour,
                                    @Param("societeId") Integer societeId,
                                    @Param("serviceId") Integer serviceId,
                                    @Param("domaineId") Integer domaineId);

    @Query("SELECT COUNT(e) FROM EcheancePaiement e WHERE e.dateEcheance < :jour AND e.montantPaye < e.montantAttendu")
    long countEnRetard(@Param("jour") LocalDate jour);

    @Query("""
            SELECT e FROM EcheancePaiement e
            WHERE e.dateEcheance >= :jour
              AND e.montantPaye < e.montantAttendu
            ORDER BY e.dateEcheance ASC
            """)
    List<EcheancePaiement> findAVenir(@Param("jour") LocalDate jour);
}
