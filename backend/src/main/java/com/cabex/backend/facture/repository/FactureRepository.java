package com.cabex.backend.facture.repository;

import com.cabex.backend.facture.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Integer> {

    List<Facture> findByDemandeId(Integer demandeId);

    Optional<Facture> findByReferenceFacture(String referenceFacture);

    boolean existsByReferenceFacture(String referenceFacture);

    long countByReferenceFactureStartingWith(String prefix);

    /** Proformas créées pour des demandes dont la date est dans l'année donnée. */
    @Query("SELECT COUNT(f) FROM Facture f WHERE YEAR(f.demande.dateDemande) = :annee")
    long countByAnneeDemande(@Param("annee") Integer annee);
}
