package com.cabex.backend.facture.repository;

import com.cabex.backend.facture.entity.FactureValideeLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FactureValideeLigneRepository extends JpaRepository<FactureValideeLigne, Integer> {

    List<FactureValideeLigne> findByFactureValideeIdOrderByIdAsc(Integer factureValideeId);

    /** Somme payée (paiements actifs) couvrant une ligne de service via les factures combinées
     *  (type PRINCIPALE uniquement : les lignes reprises « à titre de rappel » ne sont pas payables). */
    @Query("SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p " +
           "JOIN p.factureValidee.lignes fvl " +
           "WHERE p.actif = true AND fvl.ligneService.id = :ligneServiceId " +
           "AND fvl.type = 'PRINCIPALE'")
    BigDecimal sommePayeeViaFacturesCombinees(@Param("ligneServiceId") Integer ligneServiceId);
}
