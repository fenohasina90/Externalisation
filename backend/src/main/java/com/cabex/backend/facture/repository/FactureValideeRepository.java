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
     * Revenu par service selon les factures validées :
     * lignes PRINCIPALE persistées à la création de la mensuelle (mensuelle 1) et
     * des mensuelles suivantes. Repli sur les lignes du proforma validé quand
     * aucune ligne n'a été persistée pour la facture validée (demandes sans abonnement).
     * Les lignes DEBOURS ne sont rattachées à aucun service : elles sont
     * agrégées sous la fausse ligne « Débours » (serviceId 0), toujours
     * présente, indépendante du filtre service. Montant des débours = somme,
     * sur toutes les factures validées, de la part débours : lignes PRINCIPALE
     * liées à des lignes DEBOURS (mensuelles), facture rattachée directement
     * à une ligne DEBOURS, ou part DEBOURS de la proforma validée quand la
     * facture n'a ni lignes persistées ni rattachement direct.
     * Colonnes : s.id, s.nom_service, nb_demandes, revenu.
     */
    @org.springframework.data.jpa.repository.Query(value =
            "SELECT sid, nom, SUM(nb_dem) AS nb_demandes, SUM(rev) AS revenu"
            + " FROM ("
            + "   SELECT s.id AS sid, s.nom_service AS nom, COUNT(DISTINCT fv.demande_id) AS nb_dem, COALESCE(SUM(fvl.montant), 0) AS rev"
            + "   FROM facture_validee_ligne fvl"
            + "   JOIN facture_validee fv ON fv.id = fvl.facture_validee_id"
            + "   JOIN demande_service d ON d.id = fv.demande_id"
            + "   JOIN ligne_service l ON l.id = fvl.ligne_service_id"
            + "   JOIN services s ON s.id = l.service_id"
            + "   WHERE d.actif = 1"
            + "     AND l.categorie = 'EXTERNALISATION'"
            + "     AND fvl.type = 'PRINCIPALE'"
            + "     AND (:dateDebut IS NULL OR d.date_demande >= :dateDebut)"
            + "     AND (:dateFin IS NULL OR d.date_demande <= :dateFin)"
            + "     AND (:clientId IS NULL OR d.societe_id = :clientId)"
            + "     AND (:serviceId IS NULL OR l.service_id = :serviceId)"
            + "   GROUP BY s.id, s.nom_service"
            + "   UNION ALL"
            + "   SELECT s.id AS sid, s.nom_service AS nom, COUNT(DISTINCT fv.demande_id) AS nb_dem, COALESCE(SUM(fpl.montant_htva), 0) AS rev"
            + "   FROM facture_proforma fp"
            + "   JOIN facture_proforma_ligne fpl ON fpl.facture_proforma_id = fp.id"
            + "   JOIN facture_validee fv ON fv.facture_proforma_id = fp.id"
            + "   JOIN demande_service d ON d.id = fv.demande_id"
            + "   JOIN ligne_service l ON l.id = fpl.ligne_service_id"
            + "   JOIN services s ON s.id = l.service_id"
            + "   WHERE d.actif = 1"
            + "     AND fpl.categorie = 'EXTERNALISATION'"
            + "     AND fpl.type_prestation <> 'ABONNEMENT_MENSUEL'"
            + "     AND fp.statut = 'VALIDEE'"
            + "     AND l.tarif IS NOT NULL"
            + "     AND NOT EXISTS (SELECT 1 FROM facture_validee_ligne x WHERE x.facture_validee_id = fv.id)"
            + "     AND (:dateDebut IS NULL OR d.date_demande >= :dateDebut)"
            + "     AND (:dateFin IS NULL OR d.date_demande <= :dateFin)"
            + "     AND (:clientId IS NULL OR d.societe_id = :clientId)"
            + "     AND (:serviceId IS NULL OR l.service_id = :serviceId)"
            + "   GROUP BY s.id, s.nom_service"
            + "   UNION ALL"
            + "   SELECT 0 AS sid, 'Débours' AS nom, COUNT(DISTINCT demande_id) AS nb_dem, COALESCE(SUM(montant_debours), 0) AS rev"
            + "   FROM ("
            + "     SELECT fv.demande_id AS demande_id, fvl.montant AS montant_debours"
            + "     FROM facture_validee_ligne fvl"
            + "     JOIN facture_validee fv ON fv.id = fvl.facture_validee_id"
            + "     JOIN ligne_service l ON l.id = fvl.ligne_service_id"
            + "     WHERE l.categorie = 'DEBOURS' AND fvl.type = 'PRINCIPALE'"
            + "     UNION ALL"
            + "     SELECT fv.demande_id, fv.montant"
            + "     FROM facture_validee fv"
            + "     JOIN ligne_service l ON l.id = fv.ligne_service_id"
            + "     WHERE l.categorie = 'DEBOURS'"
            + "       AND NOT EXISTS (SELECT 1 FROM facture_validee_ligne x WHERE x.facture_validee_id = fv.id)"
            + "     UNION ALL"
            + "     SELECT fv.demande_id, fpl.montant_htva"
            + "     FROM facture_proforma fp"
            + "     JOIN facture_proforma_ligne fpl ON fpl.facture_proforma_id = fp.id"
            + "     JOIN ligne_service l ON l.id = fpl.ligne_service_id"
            + "     JOIN facture_validee fv ON fv.facture_proforma_id = fp.id"
            + "     WHERE l.categorie = 'DEBOURS' AND fp.statut = 'VALIDEE'"
            + "       AND fv.ligne_service_id IS NULL"
            + "       AND NOT EXISTS (SELECT 1 FROM facture_validee_ligne x WHERE x.facture_validee_id = fv.id)"
            + "   ) t"
            + "   JOIN demande_service d ON d.id = t.demande_id"
            + "   WHERE d.actif = 1"
            + "     AND (:dateDebut IS NULL OR d.date_demande >= :dateDebut)"
            + "     AND (:dateFin IS NULL OR d.date_demande <= :dateFin)"
            + "     AND (:clientId IS NULL OR d.societe_id = :clientId)"
            + " ) u"
            + " GROUP BY sid, nom"
            + " ORDER BY revenu DESC",
            nativeQuery = true)
    List<Object[]> sommeServiceFacturesValidees(@org.springframework.data.repository.query.Param("dateDebut") java.time.LocalDate dateDebut,
                                                 @org.springframework.data.repository.query.Param("dateFin") java.time.LocalDate dateFin,
                                                 @org.springframework.data.repository.query.Param("clientId") Integer clientId,
                                                 @org.springframework.data.repository.query.Param("serviceId") Integer serviceId);

    /**
     * Agrégats par facture validée pour un lot de demandes (situation de règlement).
     * Colonnes : demande.id, f.id, COALESCE(montant, 0), paye payé actif (COALESCE 0),
     * f.valideLe, f.mois.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT f.demande.id, f.id, COALESCE(f.montant, 0),
                   (SELECT COALESCE(SUM(p.montantPaye), 0) FROM Paiement p
                    WHERE p.factureValidee = f AND p.actif = true),
                   f.valideLe, f.mois
            FROM FactureValidee f
            WHERE f.demande.id IN :ids
            ORDER BY f.demande.id, f.id
            """)
    List<Object[]> facturesAgregees(@org.springframework.data.repository.query.Param("ids") List<Integer> ids);

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
     * Montant total — somme des montants de toutes les factures validées de la
     * période, tous types confondus (débours inclus). Filtrable par client et
     * par service (les demandes contenant le service).
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(fv.montant), 0)"
                    + " FROM com.cabex.backend.facture.entity.FactureValidee fv"
                    + " JOIN fv.demande d"
                    + " LEFT JOIN d.societe s"
                    + " WHERE d.actif = true"
                    + "   AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)"
                    + "   AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)"
                    + "   AND (:clientId IS NULL OR s.id = :clientId)"
                    + "   AND (:serviceId IS NULL OR EXISTS ("
                    + "           SELECT 1 FROM com.cabex.backend.demande.entity.LigneService l"
                    + "           WHERE l.demande = d AND l.service.id = :serviceId))")
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
