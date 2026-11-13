package com.cabex.backend.facture.repository;

import com.cabex.backend.facture.entity.FactureProformaLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FactureProformaLigneRepository
        extends JpaRepository<FactureProformaLigne, Integer> {

    List<FactureProformaLigne> findByFactureProformaIdOrderByOrdreAsc(Integer factureProformaId);

    @Modifying
    @Query("DELETE FROM FactureProformaLigne fpl WHERE fpl.factureProforma.id = :factureProformaId")
    void deleteByFactureProformaId(@Param("factureProformaId") Integer factureProformaId);
}
