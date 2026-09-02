package com.cabex.backend.paiement.repository;

import com.cabex.backend.paiement.entity.Paiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Integer> {

    Optional<Paiement> findByReferencePaiement(String referencePaiement);

    boolean existsByReferencePaiement(String referencePaiement);

    long count();

    @Query("SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p")
    BigDecimal sommeTotalePayee();

    @Query("SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p WHERE p.demande.id = :demandeId")
    BigDecimal sommePayeeParDemande(@Param("demandeId") Integer demandeId);

    @Query("SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p WHERE p.echeance.id = :echeanceId")
    BigDecimal sommePayeeParEcheance(@Param("echeanceId") Integer echeanceId);

    @Query("""
        SELECT p FROM Paiement p
        WHERE (:recherche IS NULL OR LOWER(p.referencePaiement) LIKE LOWER(CONCAT('%', :recherche, '%')))
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
}
