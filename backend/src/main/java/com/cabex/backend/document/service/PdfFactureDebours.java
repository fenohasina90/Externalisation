package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.facture.entity.FactureValidee;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Facture validée de débours (sans proforma préalable).
 * Facture simple : une ligne de désignation reprenant le libellé validé.
 */
final class PdfFactureDebours {

    private PdfFactureDebours() {
    }

    static byte[] generer(FactureValidee facture) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT, "Facture",
                "Facture " + PdfFormats.valeur(facture.getReferenceFactureValidee()),
                doc -> ecrire(doc, facture),
                "Erreur de génération de la facture de débours");
    }

    private static void ecrire(Document doc, FactureValidee facture) {
        PdfComposants.titreDocument(doc, "Facture",
                "Référence : " + PdfFormats.valeur(facture.getReferenceFactureValidee()));
        doc.add(bandeauMontant(facture));
        cartes(doc, facture);
        detail(doc, facture);
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
        String client = facture.getDemande() != null && facture.getDemande().getSociete() != null
                ? facture.getDemande().getSociete().getNomSociete()
                : null;
        LigneService ligne = facture.getLigneService();
        String designation = designationLigne(ligne);

        PdfPCell cClient = PdfComposants.carte("Client et demande", new String[][]{
                {"Client", PdfFormats.valeur(client)},
                {"Demande", PdfFormats.valeur(facture.getDemande() != null
                        ? facture.getDemande().getReferenceDemande() : null)}
        });
        PdfPCell cReferences = PdfComposants.carte("Références facturation", new String[][]{
                {"Facture validée", PdfFormats.valeur(facture.getReferenceFactureValidee())},
                {"Facture proforma", "— (facture sans proforma)"},
                {"Ligne", PdfFormats.valeur(designation)}
        });
        PdfPCell cValidation = PdfComposants.carte("Validation", new String[][]{
                {"Émise le", facture.getValideLe() != null
                        ? PdfFormats.valeurDate(facture.getValideLe().toLocalDate()) : null},
                {"Émise par", PdfFormats.valeur(facture.getValidePar() != null
                        ? PdfFormats.nomComplet(facture.getValidePar().getPrenom(),
                                facture.getValidePar().getNom()) : null)}
        });

        PdfComposants.rangee(doc, 0.5f, cClient, cReferences);
        PdfComposants.rangee(doc, 0.5f, cValidation,
                PdfComposants.carte("Mode de règlement", new String[][]{
                        {"Paiement lié à", "Facture validée"},
                        {"Échéance", "Aucune"}
                }));
    }

    private static String designationLigne(LigneService ligne) {
        if (ligne == null) {
            return null;
        }
        if (ligne.getDesignationDebours() != null && !ligne.getDesignationDebours().isBlank()) {
            return "Débours — " + ligne.getDesignationDebours();
        }
        return "Débours";
    }

    private static void detail(Document doc, FactureValidee facture) {
        LigneService ligne = facture.getLigneService();
        String designation = designationLigne(ligne);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.4f});
        table.setSpacingBefore(10);

        PdfPCell entete = new PdfPCell(new com.lowagie.text.Phrase("DÉTAIL DE LA FACTURE", PdfTheme.F_SECTION));
        entete.setColspan(2);
        entete.setBorder(Rectangle.BOTTOM);
        entete.setBorderColor(PdfTheme.PRIMAIRE);
        entete.setBorderWidth(1f);
        entete.setPaddingLeft(0);
        entete.setPaddingBottom(4);
        table.addCell(entete);

        PdfPCell h1 = new PdfPCell(new com.lowagie.text.Phrase("Désignation", PdfTheme.F_LABEL));
        h1.setBorder(Rectangle.NO_BORDER);
        h1.setPadding(6);
        table.addCell(h1);
        PdfPCell h2 = new PdfPCell(new com.lowagie.text.Phrase("Montant (Ariary)", PdfTheme.F_LABEL));
        h2.setBorder(Rectangle.NO_BORDER);
        h2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        h2.setPadding(6);
        table.addCell(h2);

        PdfPCell c1 = new PdfPCell(new com.lowagie.text.Phrase(
                PdfFormats.valeur(designation), PdfTheme.F_VALEUR));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(7);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new com.lowagie.text.Phrase(
                PdfFormats.formatMontant(facture.getMontant()), PdfTheme.F_VALEUR));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(7);
        table.addCell(c2);

        PdfPCell totalLibelle = new PdfPCell(new com.lowagie.text.Phrase("TOTAL À PAYER", PdfTheme.F_SECTION));
        totalLibelle.setBorder(Rectangle.TOP);
        totalLibelle.setBorderColor(PdfTheme.PRIMAIRE);
        totalLibelle.setBorderWidth(1f);
        totalLibelle.setPadding(8);
        table.addCell(totalLibelle);
        PdfPCell totalMontant = new PdfPCell(new com.lowagie.text.Phrase(
                PdfFormats.formatMontant(facture.getMontant()), PdfTheme.F_VALEUR));
        totalMontant.setBorder(Rectangle.TOP);
        totalMontant.setBorderColor(PdfTheme.PRIMAIRE);
        totalMontant.setBorderWidth(1f);
        totalMontant.setPadding(8);
        totalMontant.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalMontant);

        doc.add(table);
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
