package com.cabex.backend.reinitialisation.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.reinitialisation.dto.ReinitialisationRequest;
import com.cabex.backend.reinitialisation.dto.ReinitialisationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Suppression définitive des données métier sélectionnées depuis
 * l'administration (« Réinitialisation de données »).
 *
 * <p>Règles de cascade (les clés étrangères RESTRICT imposent l'ordre ;
 * suppressions physiques uniquement, aucun archive) :</p>
 * <ul>
 *   <li>{@code paiement} : paiements + pièces jointes rattachées ;</li>
 *   <li>{@code demande} : demandes + tout ce qui en dépend — lignes,
 *       dossiers (création / transaction / associé), factures, paiements,
 *       plans de paiement proposés et échéances, historiques, pièces ;</li>
 *   <li>{@code facture} : factures validées et proformas (lignes comprises)
 *       + paiements qui les règlent (et pièces rattachées) ;</li>
 *   <li>{@code service} : services + lignes externes qui les utilisent, et
 *       tout ce qui dépend de ces lignes (lignes de facturation validées,
 *       factures, paiements, plans, historiques de lignes). Les demandes
 *       elles-mêmes ne sont pas supprimées ;</li>
 *   <li>{@code domaine} : domaines + leurs services, mêmes cascades que
 *       {@code service} ;</li>
 *   <li>{@code client} : sociétés + tout ce qui dépend d'elles (les demandes
 *       et leurs cascades complètes, responsables client compris via la
 *       cascade de la base) ;</li>
 *   <li>{@code notification}, {@code piece_jointe}, {@code responsable_client},
 *       {@code commerciaux} : suppression directe de la table.</li>
 * </ul>
 *
 * <p>Le journal d'audit et la configuration ne sont jamais touchés.</p>
 */
@Service
public class ReinitialisationService {

    private static final Logger log = LoggerFactory.getLogger(ReinitialisationService.class);

    /** Mot de confirmation attendu côté client. */
    public static final String CONFIRMATION = "SUPPRIMER";

    /** Cibles acceptées dans la requête (codes utilisés par le frontend). */
    public enum Cible {
        PAIEMENT, DEMANDE, FACTURE, NOTIFICATION, SERVICE, DOMAINE,
        CLIENT, PIECE_JOINTE, RESPONSABLE_CLIENT, COMMERCIAUX;

        static Cible fromCode(String code) {
            String normalise = code == null ? "" : code.trim().toUpperCase().replace('-', '_');
            try {
                return Cible.valueOf(normalise);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Cible inconnue : « " + code + " ».");
            }
        }
    }

    private final JdbcTemplate jdbcTemplate;
    private final AuditService audit;

    public ReinitialisationService(JdbcTemplate jdbcTemplate, AuditService audit) {
        this.jdbcTemplate = jdbcTemplate;
        this.audit = audit;
    }

    @Transactional
    public ReinitialisationResponse reinitialiser(ReinitialisationRequest request) {
        validateRequest(request);
        List<String> ciblesBrut = request.cibles();
        Set<Cible> cibles = EnumSet.noneOf(Cible.class);
        for (String code : ciblesBrut) {
            cibles.add(Cible.fromCode(code));
        }

        boolean demande = cibles.contains(Cible.DEMANDE) || cibles.contains(Cible.CLIENT);
        boolean facture = cibles.contains(Cible.FACTURE)
                || demande
                || cibles.contains(Cible.SERVICE)
                || cibles.contains(Cible.DOMAINE);
        boolean paiement = cibles.contains(Cible.PAIEMENT) || facture;
        // Lignes externes : supprimées par demande, service ou domaine (factures en découlent).
        boolean lignes = facture || demande;
        boolean notification = cibles.contains(Cible.NOTIFICATION);
        boolean service = cibles.contains(Cible.SERVICE) || cibles.contains(Cible.DOMAINE);
        boolean domaine = cibles.contains(Cible.DOMAINE);
        boolean commercial = cibles.contains(Cible.COMMERCIAUX);
        boolean responsable = cibles.contains(Cible.RESPONSABLE_CLIENT) || cibles.contains(Cible.CLIENT);
        // Pièces : tout effacer si demande ou paiement supprimés, sinon celles liées aux paiements effacés.
        boolean pieceJointeFull = cibles.contains(Cible.PIECE_JOINTE) || demande || paiement;

        Map<String, Long> compteurs = new LinkedHashMap<>();

        // 1. Fichiers des pièces jointes qui vont disparaître : lister avant suppression.
        List<String> cheminsPieces = List.of();
        if (pieceJointeFull) {
            cheminsPieces = jdbcTemplate.queryForList(
                    "SELECT chemin_fichier FROM piece_jointe", String.class);
        } else if (facture) {
            cheminsPieces = jdbcTemplate.queryForList(
                    "SELECT chemin_fichier FROM piece_jointe WHERE paiement_id IS NOT NULL", String.class);
        }

        // 2. Ordre imposé par les clés étrangères RESTRICT.
        runIf(compteurs, pieceJointeFull || facture, "piece_jointe",
                pieceJointeFull
                        ? "DELETE FROM piece_jointe"
                        : "DELETE FROM piece_jointe WHERE paiement_id IS NOT NULL");
        runIf(compteurs, paiement, "paiement", "DELETE FROM paiement");
        runIf(compteurs, lignes, "echeance_paiement", "DELETE FROM echeance_paiement");
        runIf(compteurs, lignes, "propos_paiement", "DELETE FROM propos_paiement");
        runIf(compteurs, facture, "facture_validee_ligne", "DELETE FROM facture_validee_ligne");
        runIf(compteurs, facture, "facture_validee", "DELETE FROM facture_validee");
        runIf(compteurs, facture || demande, "facture_proforma_ligne", "DELETE FROM facture_proforma_ligne");
        runIf(compteurs, facture || demande, "facture_proforma", "DELETE FROM facture_proforma");
        runIf(compteurs, demande, "dossier_transaction_immobiliere",
                "DELETE FROM dossier_transaction_immobiliere");
        runIf(compteurs, demande, "dossier_associe", "DELETE FROM dossier_associe");
        runIf(compteurs, demande, "dossier_creation_entreprise", "DELETE FROM dossier_creation_entreprise");
        runIf(compteurs, demande, "historique_demande", "DELETE FROM historique_demande");
        runIf(compteurs, lignes, "historique_ligne_service", "DELETE FROM historique_ligne_service");
        runIf(compteurs, lignes, "ligne_service", "DELETE FROM ligne_service");
        runIf(compteurs, demande, "demande_service", "DELETE FROM demande_service");
        runIf(compteurs, notification, "notification", "DELETE FROM notification");
        runIf(compteurs, service, "services", "DELETE FROM services");
        runIf(compteurs, domaine, "domaine_service", "DELETE FROM domaine_service");
        runIf(compteurs, cibles.contains(Cible.CLIENT), "societe", "DELETE FROM societe");
        runIf(compteurs, responsable, "client_responsable", "DELETE FROM client_responsable");
        runIf(compteurs, commercial, "commercial", "DELETE FROM commercial");

        // 3. Fichiers sur disque : suppression best-effort après le DELETE, hors transaction pour les IO.
        long fichiersSupprimes = 0;
        long fichiersIntrouvables = 0;
        for (String chemin : cheminsPieces) {
            try {
                if (Files.deleteIfExists(Path.of(chemin))) {
                    fichiersSupprimes++;
                } else {
                    fichiersIntrouvables++;
                }
            } catch (IOException e) {
                log.warn("Impossible de supprimer le fichier de pièce jointe {} : {}", chemin, e.getMessage());
                fichiersIntrouvables++;
            }
        }

        String resume = resumeCompteurs(compteurs, fichiersSupprimes);
        audit.log(AuditActions.RESET_DATA, "REINITIALISATION", "Donnees", null, null, resume);
        log.info("Réinitialisation de données exécutée : {}", resume);
        return new ReinitialisationResponse(compteurs, fichiersSupprimes, fichiersIntrouvables);
    }

    private void validateRequest(ReinitialisationRequest request) {
        if (request == null) {
            throw new BusinessException("Requête invalide.");
        }
        if (!CONFIRMATION.equals(request.confirmation())) {
            throw new BusinessException("La confirmation doit être le mot « " + CONFIRMATION + " ».");
        }
        if (request.cibles() == null || request.cibles().isEmpty()) {
            throw new BusinessException("Sélectionnez au moins une donnée à supprimer.");
        }
    }

    private void runIf(Map<String, Long> compteurs, boolean actif, String table, String sql) {
        if (!actif) {
            return;
        }
        long lignes = jdbcTemplate.update(sql);
        compteurs.put(table, lignes);
    }

    private String resumeCompteurs(Map<String, Long> compteurs, long fichiersSupprimes) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entree : compteurs.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(entree.getKey()).append('=').append(entree.getValue());
        }
        sb.append(" ; fichiers=").append(fichiersSupprimes);
        return sb.toString();
    }
}
