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

    /**
     * Ancienne donnée : facture validée sans proforma (ancien modèle DEBOURS),
     * non liée à une ligne de service, pour une demande donnée.
     * Retournée en {@code Optional} (au plus une) — les factures enfants
     * (mode UNIQUE) et les factures de l'abonnement mensuel sont pourtant
     * plusieurs à partager la demande avec la même signature sinon, d'où
     * un résultat non unique susceptible de faire échouer l'appelant.
     */
    Optional<com.cabex.backend.facture.entity.FactureValidee>
            findFirstByDemandeIdAndFactureProformaIdIsNullAndLigneServiceIdIsNull(Integer demandeId);

    List<com.cabex.backend.facture.entity.FactureValidee> findByDemandeIdOrderByIdAsc(Integer demandeId);

    List<com.cabex.backend.facture.entity.FactureValidee> findByLigneServiceId(Integer ligneServiceId);

    /** Facture du mois d'un abonnement pour un mois donné (YYYY-MM-01). */
    com.cabex.backend.facture.entity.FactureValidee findByLigneServiceIdAndMois(Integer ligneServiceId, java.time.LocalDate mois);
}
