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
     * Toutes les factures validées liées à un client, de la plus ancienne à la plus
     * récente. Charge la demande et la société, la ligne de service et son service
     * pour la composition de l'onglet « Facturation » de la fiche client.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT fv FROM FactureValidee fv"
                    + " JOIN FETCH fv.demande d JOIN FETCH d.societe s"
                    + " LEFT JOIN FETCH fv.ligneService l LEFT JOIN FETCH l.service svc"
                    + " LEFT JOIN FETCH fv.factureGenerale fg"
                    + " WHERE s.id = :societeId"
                    + " ORDER BY fv.valideLe ASC")
    List<com.cabex.backend.facture.entity.FactureValidee> findBySocieteId(
            @org.springframework.data.repository.query.Param("societeId") Integer societeId);

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

    /** Mensualités créées pour une demande (renseigne le mois), de la plus ancienne à la plus récente. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT f FROM FactureValidee f WHERE f.demande.id = :demandeId AND f.mois IS NOT NULL ORDER BY f.valideLe ASC")
    List<com.cabex.backend.facture.entity.FactureValidee> findByDemandeIdAndMoisIsNotNull(
            @org.springframework.data.repository.query.Param("demandeId") Integer demandeId);

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
                    + " WHERE f.mois IS NULL")
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

    /**
     * Factures validées du mois liées à une ligne d'abonnement :
     * celles directement rattachées (ligneServiceId) et celles
     * multi-lignes dont cette ligne apparaît parmi les lignes.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT DISTINCT f FROM com.cabex.backend.facture.entity.FactureValidee f"
                    + " LEFT JOIN f.lignes fl"
                    + " WHERE f.mois IS NOT NULL"
                    + "   AND (f.ligneService.id = :ligneId"
                    + "        OR fl.ligneService.id = :ligneId)")
    List<com.cabex.backend.facture.entity.FactureValidee> findMensuellesPourLigne(
            @org.springframework.data.repository.query.Param("ligneId") Integer ligneId);

    /**
     * Montant facturé d'une demande = somme des tarifs validés de ses lignes
     * EXTERNALISATION (prestation, abonnement, création, transaction).
     * Toute facture validée est payable (mensuelles incluses).
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(COALESCE(l.tarif, 0)), 0)"
                    + " FROM com.cabex.backend.demande.entity.LigneService l"
                    + " WHERE l.demande.id = :demandeId"
                    + "   AND l.categorie <> 'DEBOURS'"
                    + "   AND l.tarif IS NOT NULL"
                    + "   AND l.facture IS NOT NULL"
                    + "   AND l.facture.statut = 'VALIDEE'")
    java.math.BigDecimal sommeFacturesPaieablesPourDemande(@org.springframework.data.repository.query.Param("demandeId") Integer demandeId);

    /**
     * Montant facturé (payable) — somme des tarifs validés des lignes EXTERNALISATION
     * rattachées à un proforma validé. Débours exclus ; prestations et abonnements comptent.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(COALESCE(l.tarif, 0)), 0)"
                    + " FROM com.cabex.backend.demande.entity.LigneService l"
                    + " JOIN l.demande d"
                    + " LEFT JOIN d.societe s"
                    + " LEFT JOIN l.service svc"
                    + " WHERE d.actif = true"
                    + "   AND l.categorie <> 'DEBOURS'"
                    + "   AND l.tarif IS NOT NULL"
                    + "   AND l.facture IS NOT NULL"
                    + "   AND l.facture.statut = 'VALIDEE'"
                    + "   AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)"
                    + "   AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)"
                    + "   AND (:clientId IS NULL OR s.id = :clientId)"
                    + "   AND (:serviceId IS NULL OR svc.id = :serviceId)")
    java.math.BigDecimal sommeMontantFacturesPayables(@org.springframework.data.repository.query.Param("dateDebut") java.time.LocalDate dateDebut,
                                                      @org.springframework.data.repository.query.Param("dateFin") java.time.LocalDate dateFin,
                                                      @org.springframework.data.repository.query.Param("clientId") Integer clientId,
                                                      @org.springframework.data.repository.query.Param("serviceId") Integer serviceId);

    /**
     * Montant validé (payable) par client, pour les rapports et le tableau de bord.
     * Colonnes : id, nom société, nombre de demandes actives, montant total.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT s.id, s.nomSociete, COUNT(DISTINCT d.id), COALESCE(SUM(COALESCE(l.tarif, 0)), 0)"
                    + " FROM DemandeService d"
                    + " LEFT JOIN d.societe s"
                    + " JOIN com.cabex.backend.demande.entity.LigneService l ON l.demande = d"
                    + " LEFT JOIN l.service svc"
                    + " WHERE d.actif = true"
                    + "   AND l.categorie <> 'DEBOURS'"
                    + "   AND l.tarif IS NOT NULL"
                    + "   AND l.facture IS NOT NULL"
                    + "   AND l.facture.statut = 'VALIDEE'"
                    + "   AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)"
                    + "   AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)"
                    + "   AND (:clientId IS NULL OR s.id = :clientId)"
                    + "   AND (:serviceId IS NULL OR EXISTS ("
                    + "           SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l2"
                    + "           WHERE l2.demande = d AND l2.service.id = :serviceId))"
                    + " GROUP BY s.id, s.nomSociete"
                    + " ORDER BY COUNT(DISTINCT d.id) DESC")
    List<Object[]> sommeMontantFacturesParClient(@org.springframework.data.repository.query.Param("dateDebut") java.time.LocalDate dateDebut,
                                                 @org.springframework.data.repository.query.Param("dateFin") java.time.LocalDate dateFin,
                                                 @org.springframework.data.repository.query.Param("clientId") Integer clientId,
                                                 @org.springframework.data.repository.query.Param("serviceId") Integer serviceId);

    /**
     * Montant facturé par service.
     * Colonnes : id, nom service, nombre de demandes actives, montant total.
     * Attribution par ligne EXTERNALISATION d'un proforma validé (tarifs validés) :
     * prestation, abonnement, création et transaction sont comptés, débours exclus.
     */
    @org.springframework.data.jpa.repository.Query(
            nativeQuery = true,
            value = "SELECT s.id AS sid, s.nom_service AS nom,"
                    + "       COUNT(DISTINCT d.id) AS nb_demandes,"
                    + "       COALESCE(SUM(COALESCE(l.tarif, 0)), 0) AS revenu"
                    + " FROM demande_service d"
                    + " JOIN ligne_service l ON l.demande_id = d.id"
                    + " JOIN facture_proforma fp ON fp.id = l.facture_id AND fp.statut = 'VALIDEE'"
                    + " JOIN services s ON s.id = l.service_id"
                    + " WHERE d.actif = 1"
                    + "   AND l.categorie <> 'DEBOURS'"
                    + "   AND l.tarif IS NOT NULL"
                    + "   AND (:dateDebut IS NULL OR d.date_demande >= :dateDebut)"
                    + "   AND (:dateFin IS NULL OR d.date_demande <= :dateFin)"
                    + "   AND (:clientId IS NULL OR d.societe_id = :clientId)"
                    + "   AND (:serviceId IS NULL OR s.id = :serviceId)"
                    + " GROUP BY s.id, s.nom_service"
                    + " ORDER BY revenu DESC")
    List<Object[]> sommeMontantFacturesParService(@org.springframework.data.repository.query.Param("dateDebut") java.time.LocalDate dateDebut,
                                                  @org.springframework.data.repository.query.Param("dateFin") java.time.LocalDate dateFin,
                                                  @org.springframework.data.repository.query.Param("clientId") Integer clientId,
                                                  @org.springframework.data.repository.query.Param("serviceId") Integer serviceId);
}
