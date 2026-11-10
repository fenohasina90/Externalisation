package com.cabex.backend.facture.repository;

import com.cabex.backend.facture.entity.FactureValidee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FactureValideeRepository extends JpaRepository<FactureValidee, Integer> {

    Optional<FactureValidee> findByFactureProformaId(Integer factureProformaId);

    boolean existsByFactureProformaId(Integer factureProformaId);

    boolean existsByReferenceFactureValidee(String referenceFactureValidee);

    long countByReferenceFactureValideeEndingWith(String suffixe);

    /** Facture validée sans proforma (cas DEBOURS) pour une demande donnée. */
    com.cabex.backend.facture.entity.FactureValidee findByFactureProformaIdIsNullAndDemandeId(Integer demandeId);

    List<com.cabex.backend.facture.entity.FactureValidee> findByDemandeIdOrderByIdAsc(Integer demandeId);

    List<com.cabex.backend.facture.entity.FactureValidee> findByLigneServiceId(Integer ligneServiceId);
}
