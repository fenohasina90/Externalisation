package com.cabex.backend.document.service;

import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.facture.dto.FactureValideeFicheResponse;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.client.entity.Societe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Génération des documents PDF du cabinet : récapitulatif de demande, fiches de
 * création et de transaction immobilière, reçu de paiement, plan de paiement et
 * facture proforma.
 *
 * <p>Point d'entrée du service ; la mise en page est déléguée aux générateurs
 * spécialisés ({@link PdfRecapitulatif}, {@link PdfFicheCreation},
 * {@link PdfFicheTransactionImmobiliere}, {@link PdfRecuPaiement},
 * {@link PdfPlanPaiement}, {@link PdfFactureProforma},
 * {@link PdfFactureAbonnement}) et la charte graphique
 * est centralisée dans {@link PdfTheme}.</p>
 */
@Component
public class PdfGenerator {

    private final PdfFactureProforma generateurFacture;

    private final PdfFacturePaiement generateurFacturePaiement;

    private final PdfFactureAbonnement generateurFactureAbonnement;

    private final PaiementRepository paiementRepository;

    public PdfGenerator(ConfigurationService configurationService) {
        this(configurationService, null);
    }

    @Autowired
    public PdfGenerator(ConfigurationService configurationService,
                        PaiementRepository paiementRepository) {
        this.generateurFacture = new PdfFactureProforma(configurationService);
        this.generateurFacturePaiement = new PdfFacturePaiement(configurationService);
        this.generateurFactureAbonnement = new PdfFactureAbonnement(configurationService);
        this.paiementRepository = paiementRepository;
    }

    /**
     * Fiche récapitulative d'une demande d'externalisation : titre + badge, bandeau
     * références, cartes Entreprise / Contact, profil chiffré, domaine, tableau des
     * services souhaités, délai, observation et signatures.
     */
    public byte[] recapitulatifDemande(DemandeService demande) {
        return PdfRecapitulatif.generer(demande);
    }

    /**
     * Fiche de création d'entreprise ou de société (reprend la fiche papier).
     */
    public byte[] ficheCreation(DemandeService demande, DossierCreation dossier) {
        return PdfFicheCreation.generer(demande, dossier);
    }

    /**
     * Fiche de transaction immobilière (reprend la fiche papier du cabinet).
     */
    public byte[] ficheTransactionImmobiliere(DemandeService demande,
                                              DossierTransactionImmobiliere dossier) {
        return PdfFicheTransactionImmobiliere.generer(demande, dossier);
    }

    /** Reçu de paiement standard. Le total payé sur la facture est recalculé si fourni. */
    /** Export PDF de la facturation client (onglet Facturation), avec filtre de dates appliqué. */
    public byte[] facturationClientPdf(Societe societe,
                                       List<FactureValideeFicheResponse> fiches,
                                       LocalDate dateDebut, LocalDate dateFin, String ordre,
                                       String recherche) {
        return PdfFacturationClient.generer(societe, fiches, dateDebut, dateFin, ordre, recherche);
    }

    /** Export Excel de la facturation client (onglet Facturation), avec filtre appliqué. */
    public byte[] facturationClientExcel(Societe societe,
                                         List<FactureValideeFicheResponse> fiches,
                                         LocalDate dateDebut, LocalDate dateFin, String ordre,
                                         String recherche) {
        return ExcelFacturationClient.generer(societe, fiches, dateDebut, dateFin, ordre, recherche);
    }

    public byte[] recuPaiement(Paiement paiement) {
        return PdfRecuPaiement.generer(paiement, null);
    }

    /** Reçu de paiement avec le total exact des paiements actifs sur la facture. */
    public byte[] recuPaiement(Paiement paiement, BigDecimal sommePayeeSurFacture) {
        return PdfRecuPaiement.generer(paiement, sommePayeeSurFacture);
    }

    /** Reçu de paiement avec objet précis de la facture validée (service / débours / Services facturés). */
    public byte[] recuPaiement(Paiement paiement, BigDecimal sommePayeeSurFacture,
                               com.cabex.backend.facture.dto.ObjetFactureValideeResponse objet) {
        return PdfRecuPaiement.generer(paiement, sommePayeeSurFacture, objet);
    }

    public byte[] planPaiement(ProposPaiement propos) {
        return PdfPlanPaiement.generer(propos);
    }

    /**
     * Facture proforma au format du modèle CABEX, générée sur une seule page.
     */
    /** Document proforma (toujours « FACTURE PROFORMA », même après validation). */
    public byte[] factureProforma(Facture facture) {
        return generateurFacture.generer(facture, "PROFORMA");
    }

    /**
     * Facture mensuelle d'un abonnement : références facture générale validée,
     * facture proforma et demande.
     */
    public byte[] factureAbonnement(FactureValidee facture) {
        return generateurFactureAbonnement.generer(facture, avancePourFv(facture));
    }

    /** Avance déjà reçue pour une facture validée liée au paiement. */
    private BigDecimal avancePourFv(FactureValidee facture) {
        if (paiementRepository == null) {
            return BigDecimal.ZERO;
        }
        if (facture == null || facture.getId() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal payee = paiementRepository.sommePayeeParFactureValidee(facture.getId());
        return payee != null ? payee : BigDecimal.ZERO;
    }

    /**
     * Document PDF complet d'une facture validée : facture du mois
     * d'abonnement, facture enfant d'une facture générale validée (mode
     * « facture unique »), débours sans proforma, ou proforma validée
     * (réutilise le rendu de la proforma).
     */
    public byte[] factureValideeDocument(com.cabex.backend.facture.entity.FactureValidee facture) {
        // Factures enfants liées au paiement (prestation, débours, création, transaction)
        // sauf factures du mois d'abonnement.
        if (facture.getLigneService() != null
                && !com.cabex.backend.demande.service.LigneServiceService.ABONNEMENT_MENSUEL
                        .equalsIgnoreCase(facture.getLigneService().getTypePrestation())) {
            return generateurFacturePaiement.generer(facture, avancePourFv(facture));
        }
        if (facture.getFactureGenerale() != null) {
            if (facture.getMois() != null) {
                return generateurFactureAbonnement.generer(facture, avancePourFv(facture));
            }
            return PdfFactureDetail.generer(facture);
        }
        if (facture.getFactureProforma() == null) {
            return PdfFactureDebours.generer(facture);
        }
        // Première facture validée (générale, éventuellement marquée mensuelle) :
        // le suivi des paiements doit être à jour comme sur les autres factures.
        return generateurFacture.generer(facture.getFactureProforma(), "VALIDÉE", avancePourFv(facture));
    }
}
