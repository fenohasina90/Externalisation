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
}
