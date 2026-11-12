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

    /**
     * Toutes les factures concernées par l'alerte « retard de paiement »
     * (prestation et débours) : factures enfants en mode unique + factures
     * générales par service + factures de débours sans proforma. Charge la
     * demande et la société pour la composition des messages.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT f FROM FactureValidee f"
                    + " JOIN FETCH f.demande d LEFT JOIN FETCH d.societe s"
                    + " LEFT JOIN FETCH f.ligneService l LEFT JOIN FETCH l.service svc"
                    + " LEFT JOIN f.factureProforma fp"
                    + " WHERE (f.factureGenerale IS NOT NULL AND f.mois IS NULL)"
                    + "    OR fp.modeFacturation = 'PAR_SERVICE'"
                    + "    OR f.factureProforma IS NULL")
    List<com.cabex.backend.facture.entity.FactureValidee> findFacturesPourAlertesPaiement();

    /**
     * Toutes les factures du mois d'abonnement (alertes de retard abonnement).
     * Charge la demande et la société pour la composition des messages.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT f FROM FactureValidee f"
                    + " JOIN FETCH f.demande d LEFT JOIN FETCH d.societe s"
                    + " LEFT JOIN FETCH f.ligneService l LEFT JOIN FETCH l.service svc"
                    + " WHERE f.mois IS NOT NULL")
    List<com.cabex.backend.facture.entity.FactureValidee> findFacturesMensuellesPourAlerte();
}
