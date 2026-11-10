package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.mapper.DemandeMapper;
import com.cabex.backend.facture.entity.FactureValidee;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Facture mensuelle d'un abonnement : enfant de la facture générale validée.
 * <p>Références affichées : facture générale validée, facture proforma,
 * demande, plus la référence propre de la facture mensuelle.</p>
 */
final class PdfFactureAbonnement {

    private PdfFactureAbonnement() {
    }

    static byte[] generer(FactureValidee facture) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT,
                "Facture d'abonnement mensuel",
                "Facture mensuelle " + PdfFormats.valeur(facture.getReferenceFactureValidee()),
                doc -> ecrire(doc, facture),
                "Erreur de génération de la facture d'abonnement");
    }

    private static void ecrire(Document doc, FactureValidee facture) {
        PdfComposants.titreDocument(doc, "Facture d'abonnement mensuel",
                "Référence : " + PdfFormats.valeur(facture.getReferenceFactureValidee()));
        doc.add(bandeauMontant(facture));
        cartes(doc, facture);
        montantEnLettres(doc, facture);
        signatures(doc, facture);
    }

    private static PdfPTable bandeauMontant(FactureValidee facture) {
        PdfPCell montant = new PdfPCell();
        montant.setBackgroundColor(PdfTheme.PRIMAIRE);
        montant.setBorder(Rectangle.NO_BORDER);
        montant.setPadding(16);

        Paragraph libelle = new Paragraph("MONTANT À PAYER", PdfTheme.F_LABEL_BLANC);
        libelle.setAlignment(Element.ALIGN_CENTER);
        montant.addElement(libelle);

        Paragraph valeur = new Paragraph(PdfFormats.formatArgent(facture.getMontant()),
                PdfTheme.F_MONTANT_XL);
        valeur.setAlignment(Element.ALIGN_CENTER);
        valeur.setSpacingBefore(4);
        montant.addElement(valeur);

        PdfPTable bandeau = new PdfPTable(1);
        bandeau.setWidthPercentage(100);
        bandeau.setSpacingAfter(14);
        bandeau.addCell(montant);
        return bandeau;
    }

    private static void cartes(Document doc, FactureValidee facture) {
        FactureValidee generale = facture.getFactureGenerale();
        String refProforma = generale != null && generale.getFactureProforma() != null
                ? generale.getFactureProforma().getReferenceFacture()
                : null;
        String refGenerale = generale != null ? generale.getReferenceFactureValidee() : null;

        String client = facture.getDemande() != null && facture.getDemande().getSociete() != null
                ? facture.getDemande().getSociete().getNomSociete()
                : null;
        LigneService ligne = facture.getLigneService();
        String service = ligne != null && ligne.getService() != null
                ? ligne.getService().getNomService() : null;
        String designation = ligne != null ? DemandeMapper.libelleLigne(ligne) : "Abonnement mensuel";

        PdfPCell cClient = PdfComposants.carte("Client et demande", new String[][]{
                {"Client", PdfFormats.valeur(client)},
                {"Demande", PdfFormats.valeur(facture.getDemande() != null
                        ? facture.getDemande().getReferenceDemande() : null)}
        });
        PdfPCell cDesignation = PdfComposants.carte("Prestation", new String[][]{
                {"Service", PdfFormats.valeur(service)},
                {"Désignation", PdfFormats.valeur(designation)},
                {"Tarif mensuel", PdfFormats.formatArgent(facture.getMontant())}
        });
        PdfPCell cFactures = PdfComposants.carte("Références facturation", new String[][]{
                {"Facture mensuelle", PdfFormats.valeur(facture.getReferenceFactureValidee())},
                {"Facture générale validée", PdfFormats.valeur(refGenerale)},
                {"Facture proforma", PdfFormats.valeur(refProforma)}
        });
        PdfPCell cValidation = PdfComposants.carte("Validation", new String[][]{
                {"Émise le", facture.getValideLe() != null
                        ? PdfFormats.valeurDate(facture.getValideLe().toLocalDate()) : null},
                {"Émise par", PdfFormats.valeur(facture.getValidePar() != null
                        ? PdfFormats.nomComplet(facture.getValidePar().getPrenom(),
                                facture.getValidePar().getNom()) : null)}
        });

        PdfComposants.rangee(doc, 0.5f, cClient, cDesignation);
        PdfComposants.rangee(doc, 0.5f, cFactures, cValidation);
    }

    private static void montantEnLettres(Document doc, FactureValidee facture) {
        if (facture.getMontant() == null) {
            return;
        }
        doc.add(PdfComposants.encartLettres("Arrêtée la présente facture à la somme de ",
                MontantEnLettres.convertir(facture.getMontant().longValue())
                        .toUpperCase(Locale.FRANCE) + " ARIARY"));
    }

    private static void signatures(Document doc, FactureValidee facture) {
        String emetteur = facture.getValidePar() != null
                ? PdfFormats.nomComplet(facture.getValidePar().getPrenom(),
                        facture.getValidePar().getNom())
                : null;

        PdfPTable signatures = new PdfPTable(2);
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(26);
        signatures.setKeepTogether(true);
        signatures.setWidths(new float[]{1f, 1f});
        signatures.addCell(PdfComposants.celluleSignature("Le client", null));
        signatures.addCell(PdfComposants.celluleSignature("Pour le cabinet",
                emetteur != null ? "Émise par " + emetteur : null));
        signatures.addCell(PdfComposants.celluleEspaceSignature(70));
        signatures.addCell(PdfComposants.celluleEspaceSignature(70));
        doc.add(signatures);
    }
}
