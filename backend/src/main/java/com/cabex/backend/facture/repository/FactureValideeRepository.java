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
     * Total des factures validées <b>payables</b> (prestation + abonnement), pour une demande unique.
     * Exclut débours et facture générale UNIQUE.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(f.montant), 0)"
                    + " FROM com.cabex.backend.facture.entity.FactureValidee f"
                    + " LEFT JOIN f.factureProforma fp"
                    + " WHERE f.demande.id = :demandeId"
                    + "   AND NOT (f.factureGenerale IS NULL"
                    + "             AND fp IS NOT NULL"
                    + "             AND fp.modeFacturation = 'UNIQUE')"
                    + "   AND NOT (f.ligneService IS NULL AND fp IS NOT NULL"
                    + "             AND EXISTS ("
                    + "                 SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l3"
                    + "                 WHERE l3.facture = fp"
                    + "                   AND l3.typePrestation = 'ABONNEMENT_MENSUEL')"
                    + "             AND NOT EXISTS ("
                    + "                 SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l4"
                    + "                 WHERE l4.facture = fp"
                    + "                   AND (l4.typePrestation IS NULL OR l4.typePrestation <> 'ABONNEMENT_MENSUEL')))"
                    + "   AND ((f.ligneService IS NOT NULL AND f.ligneService.categorie <> 'DEBOURS')"
                    + "        OR (f.ligneService IS NULL AND NOT EXISTS ("
                    + "                SELECT 1 FROM com.cabex.backend.facture.entity.FactureValideeLigne fl"
                    + "                WHERE fl.factureValidee = f"
                    + "                  AND fl.ligneService.categorie = 'DEBOURS'))"
                    + "        OR (f.ligneService IS NULL AND EXISTS ("
                    + "                SELECT 1 FROM com.cabex.backend.facture.entity.FactureValideeLigne fl"
                    + "                WHERE fl.factureValidee = f"
                    + "                  AND fl.ligneService.categorie <> 'DEBOURS')))")
    java.math.BigDecimal sommeFacturesPaieablesPourDemande(@org.springframework.data.repository.query.Param("demandeId") Integer demandeId);

    /**
     * Total des factures validées <b>payables</b> (liées à paiement : enfant du mode UNIQUE,
     * facture PAR_SERVICE, mensualité d'abonnement), <b>hors débours</b> — seules
     * les prestation et abonnement comptent (les débours sont gérés par leur propre facture, hors CA).
     * La facture générale UNIQUE (aucune cible de paiement directe) est également exclue.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(f.montant), 0)"
                    + " FROM com.cabex.backend.facture.entity.FactureValidee f"
                    + " LEFT JOIN f.factureProforma fp"
                    + " JOIN f.demande d"
                    + " LEFT JOIN d.societe s"
                    + " LEFT JOIN f.ligneService l"
                    + " LEFT JOIN l.service svc"
                    + " WHERE d.actif = true"
                    + "   AND NOT (f.factureGenerale IS NULL"
                    + "             AND fp IS NOT NULL"
                    + "             AND fp.modeFacturation = 'UNIQUE')"
                    + "   AND NOT (l IS NULL AND fp IS NOT NULL"
                    + "             AND EXISTS ("
                    + "                 SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l3"
                    + "                 WHERE l3.facture = fp"
                    + "                   AND l3.typePrestation = 'ABONNEMENT_MENSUEL')"
                    + "             AND NOT EXISTS ("
                    + "                 SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l4"
                    + "                 WHERE l4.facture = fp"
                    + "                   AND (l4.typePrestation IS NULL OR l4.typePrestation <> 'ABONNEMENT_MENSUEL')))"
                    + "   AND ((l IS NOT NULL AND l.categorie <> 'DEBOURS')"
                    + "        OR (l IS NULL AND NOT EXISTS ("
                    + "                SELECT 1 FROM com.cabex.backend.facture.entity.FactureValideeLigne fl"
                    + "                WHERE fl.factureValidee = f"
                    + "                  AND fl.ligneService.categorie = 'DEBOURS'))"
                    + "        OR (l IS NULL AND EXISTS ("
                    + "                SELECT 1 FROM com.cabex.backend.facture.entity.FactureValideeLigne fl"
                    + "                WHERE fl.factureValidee = f"
                    + "                  AND fl.ligneService.categorie <> 'DEBOURS')))"
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
            "SELECT s.id, s.nomSociete, COUNT(DISTINCT d.id), COALESCE(SUM(f.montant), 0)"
                    + " FROM DemandeService d"
                    + " LEFT JOIN d.societe s"
                    + " JOIN com.cabex.backend.facture.entity.FactureValidee f ON f.demande = d"
                    + " LEFT JOIN f.factureProforma fp"
                    + " LEFT JOIN f.ligneService l"
                    + " LEFT JOIN l.service svc"
                    + " WHERE d.actif = true"
                    + "   AND NOT (f.factureGenerale IS NULL"
                    + "             AND fp IS NOT NULL"
                    + "             AND fp.modeFacturation = 'UNIQUE')"
                    + "   AND NOT (l IS NULL AND fp IS NOT NULL"
                    + "             AND EXISTS ("
                    + "                 SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l3"
                    + "                 WHERE l3.facture = fp"
                    + "                   AND l3.typePrestation = 'ABONNEMENT_MENSUEL')"
                    + "             AND NOT EXISTS ("
                    + "                 SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l4"
                    + "                 WHERE l4.facture = fp"
                    + "                   AND (l4.typePrestation IS NULL OR l4.typePrestation <> 'ABONNEMENT_MENSUEL')))"
                    + "   AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)"
                    + "   AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)"
                    + "   AND (:clientId IS NULL OR s.id = :clientId)"
                    + "   AND (:serviceId IS NULL OR EXISTS ("
                    + "           SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l2"
                    + "           WHERE l2.demande = d AND l2.service.id = :serviceId))"
                    + "   AND ((l IS NOT NULL AND l.categorie <> 'DEBOURS')"
                    + "        OR (l IS NULL AND NOT EXISTS ("
                    + "                SELECT 1 FROM com.cabex.backend.facture.entity.FactureValideeLigne fl"
                    + "                WHERE fl.factureValidee = f"
                    + "                  AND fl.ligneService.categorie = 'DEBOURS'))"
                    + "        OR (l IS NULL AND EXISTS ("
                    + "                SELECT 1 FROM com.cabex.backend.facture.entity.FactureValideeLigne fl"
                    + "                WHERE fl.factureValidee = f"
                    + "                  AND fl.ligneService.categorie <> 'DEBOURS')))"
                    + " GROUP BY s.id, s.nomSociete"
                    + " ORDER BY COUNT(DISTINCT d.id) DESC")
    List<Object[]> sommeMontantFacturesParClient(@org.springframework.data.repository.query.Param("dateDebut") java.time.LocalDate dateDebut,
                                                 @org.springframework.data.repository.query.Param("dateFin") java.time.LocalDate dateFin,
                                                 @org.springframework.data.repository.query.Param("clientId") Integer clientId,
                                                 @org.springframework.data.repository.query.Param("serviceId") Integer serviceId);

    /**
     * Montant validé (payable) par service.
     * Colonnes : id, nom service, nombre de demandes actives, montant total.
     * Les factures générales PAR_SERVICE mono-proforma (prestation) n'ont pas
     * de ligne directe : elles sont attribuées à la ligne de leur proforma.
     * Les factures générales d'abonnement (couverture 100 % abonnement) restent
     * exclues — l'abonnement se règle sur la facture du mois.
     */
    @org.springframework.data.jpa.repository.Query(
            nativeQuery = true,
            value = "SELECT attr.sid, attr.nom, COUNT(DISTINCT attr.demande_id) AS nb_demandes,"
                    + "       COALESCE(SUM(attr.montant), 0) AS revenu"
                    + " FROM ("
                    + "         SELECT d.id AS demande_id, d.societe_id AS societe_id, d.date_demande AS date_demande,"
                    + "                s.id AS sid, s.nom_service AS nom, f.montant AS montant"
                    + "         FROM facture_validee f"
                    + "         JOIN demande_service d ON d.id = f.demande_id"
                    + "         JOIN ligne_service l ON l.id = f.ligne_service_id"
                    + "         JOIN services s ON s.id = l.service_id"
                    + "         LEFT JOIN facture_proforma fp ON fp.id = f.facture_proforma_id"
                    + "         WHERE d.actif = 1"
                    + "           AND NOT (f.facture_generale_id IS NULL AND fp.mode_facturation = 'UNIQUE')"
                    + "           AND l.categorie <> 'DEBOURS'"
                    + "         UNION ALL"
                    + "         SELECT d.id AS demande_id, d.societe_id AS societe_id, d.date_demande AS date_demande,"
                    + "                s.id AS sid, s.nom_service AS nom, f.montant AS montant"
                    + "         FROM facture_validee f"
                    + "         JOIN demande_service d ON d.id = f.demande_id"
                    + "         JOIN facture_proforma fp ON fp.id = f.facture_proforma_id"
                    + "         JOIN ligne_service lp ON lp.facture_id = fp.id"
                    + "         JOIN services s ON s.id = lp.service_id"
                    + "         WHERE d.actif = 1"
                    + "           AND fp.mode_facturation = 'PAR_SERVICE'"
                    + "           AND f.ligne_service_id IS NULL"
                    + "           AND lp.categorie <> 'DEBOURS'"
                    + "           AND (lp.type_prestation IS NULL OR lp.type_prestation <> 'ABONNEMENT_MENSUEL')"
                    + "           AND NOT EXISTS (SELECT 1 FROM facture_validee_ligne fl"
                    + "                           WHERE fl.facture_validee_id = f.id)"
                    + "       ) attr"
                    + " WHERE (:dateDebut IS NULL OR attr.date_demande >= :dateDebut)"
                    + "   AND (:dateFin IS NULL OR attr.date_demande <= :dateFin)"
                    + "   AND (:clientId IS NULL OR attr.societe_id = :clientId)"
                    + "   AND (:serviceId IS NULL OR attr.sid = :serviceId)"
                    + " GROUP BY attr.sid, attr.nom"
                    + " ORDER BY revenu DESC")
    List<Object[]> sommeMontantFacturesParService(@org.springframework.data.repository.query.Param("dateDebut") java.time.LocalDate dateDebut,
                                                  @org.springframework.data.repository.query.Param("dateFin") java.time.LocalDate dateFin,
                                                  @org.springframework.data.repository.query.Param("clientId") Integer clientId,
                                                  @org.springframework.data.repository.query.Param("serviceId") Integer serviceId);
}
