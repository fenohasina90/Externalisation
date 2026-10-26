package com.cabex.backend.facture.repository;

import com.cabex.backend.facture.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Integer> {

    List<Facture> findByDemandeId(Integer demandeId);

    Optional<Facture> findByReferenceFacture(String referenceFacture);

    boolean existsByReferenceFacture(String referenceFacture);

    long countByReferenceFactureStartingWith(String prefix);
}
