package com.cabex.backend.document.service;

import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.paiement.entity.Paiement;
import org.springframework.stereotype.Component;

/**
 * Génération des documents PDF du cabinet : récapitulatif de demande, fiche de
 * création, reçu de paiement, plan de paiement et facture proforma.
 *
 * <p>Point d'entrée du service ; la mise en page est déléguée aux générateurs
 * spécialisés ({@link PdfRecapitulatif}, {@link PdfFicheCreation},
 * {@link PdfRecuPaiement}, {@link PdfPlanPaiement}, {@link PdfFactureProforma}) et
 * la charte graphique est centralisée dans {@link PdfTheme}.</p>
 */
@Component
public class PdfGenerator {

    private final PdfFactureProforma generateurFacture;

    public PdfGenerator(ConfigurationService configurationService) {
        this.generateurFacture = new PdfFactureProforma(configurationService);
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

    public byte[] recuPaiement(Paiement paiement) {
        return PdfRecuPaiement.generer(paiement);
    }

    public byte[] planPaiement(ProposPaiement propos) {
        return PdfPlanPaiement.generer(propos);
    }

    /**
     * Facture proforma au format du modèle CABEX, générée sur une seule page.
     */
    public byte[] factureProforma(Facture facture) {
        return generateurFacture.generer(facture);
    }
}
