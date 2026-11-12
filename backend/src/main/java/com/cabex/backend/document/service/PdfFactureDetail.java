package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.mapper.DemandeMapper;
import com.cabex.backend.facture.entity.FactureValidee;

import java.util.Locale;

/**
 * Facture validée d'un service (PRESTATION / débours / création / transaction)
 * créée après la validation de la facture générale : chaque service possède
 * sa propre facture, liée au paiement, portant toujours la référence de la
 * facture générale validée.
 */
final class PdfFactureDetail {

    private PdfFactureDetail() {
    }

    static byte[] generer(FactureValidee facture) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT, "Facture",
                "Facture " + PdfFormats.valeur(facture.getReferenceFactureValidee()),
                doc -> ecrire(doc, facture),
                "Erreur de génération de la facture de service");
    }

    private static void ecrire(com.lowagie.text.Document doc, FactureValidee facture) {
        PdfComposants.titreDocument(doc, "Facture",
                "Référence : " + PdfFormats.valeur(facture.getReferenceFactureValidee()));
        doc.add(bandeauMontant(facture));
        cartes(doc, facture);
        detail(doc, facture);
        montantEnLettres(doc, facture);
        signatures(doc, facture);
    }

    private static com.lowagie.text.pdf.PdfPTable bandeauMontant(FactureValidee facture) {
        com.lowagie.text.pdf.PdfPCell montant = new com.lowagie.text.pdf.PdfPCell();
        montant.setBackgroundColor(PdfTheme.PRIMAIRE);
        montant.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        montant.setPadding(16);

        com.lowagie.text.Paragraph libelle = new com.lowagie.text.Paragraph("MONTANT À PAYER",
                PdfTheme.F_LABEL_BLANC);
        libelle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        montant.addElement(libelle);

        com.lowagie.text.Paragraph valeur = new com.lowagie.text.Paragraph(
                PdfFormats.formatArgent(facture.getMontant()), PdfTheme.F_MONTANT_XL);
        valeur.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        valeur.setSpacingBefore(4);
        montant.addElement(valeur);

        com.lowagie.text.pdf.PdfPTable bandeau = new com.lowagie.text.pdf.PdfPTable(1);
        bandeau.setWidthPercentage(100);
        bandeau.setSpacingAfter(14);
        bandeau.addCell(montant);
        return bandeau;
    }

    private static void cartes(com.lowagie.text.Document doc, FactureValidee facture) {
        String client = facture.getDemande() != null && facture.getDemande().getSociete() != null
                ? facture.getDemande().getSociete().getNomSociete()
                : null;

        PdfComposants.rangee(doc, 0.5f,
                PdfComposants.carte("Client et demande", new String[][]{
                        {"Client", PdfFormats.valeur(client)},
                        {"Demande", PdfFormats.valeur(facture.getDemande() != null
                                ? facture.getDemande().getReferenceDemande() : null)}
                }),
                PdfComposants.carte("Références facturation", new String[][]{
                        {"Facture validée", PdfFormats.valeur(facture.getReferenceFactureValidee())},
                        {"Facture générale validée", PdfFormats.valeur(generaleReference(facture))},
                        {"Facture proforma", PdfFormats.valeur(proformaReference(facture))}
                }));
        PdfComposants.rangee(doc, 0.5f,
                PdfComposants.carte("Validation", new String[][]{
                        {"Émise le", facture.getValideLe() != null
                                ? PdfFormats.valeurDate(facture.getValideLe().toLocalDate()) : null},
                        {"Émise par", PdfFormats.valeur(facture.getValidePar() != null
                                ? PdfFormats.nomComplet(facture.getValidePar().getPrenom(),
                                        facture.getValidePar().getNom()) : null)}
                }),
                PdfComposants.carte("Mode de règlement", new String[][]{
                        {"Paiement lié à", "Facture validée du service"},
                        {"Échéance", "Aucune"}
                }));
    }

    /** Référence de la facture générale validée : toujours présente, elle
     *  est reprise de la facture enfant (facture générale validée). */
    private static String generaleReference(FactureValidee facture) {
        return facture.getFactureGenerale() != null
                ? facture.getFactureGenerale().getReferenceFactureValidee()
                : "—";
    }

    /** Référence de la proforma d'origine (via la facture générale validée). */
    private static String proformaReference(FactureValidee facture) {
        if (facture.getFactureGenerale() != null
                && facture.getFactureGenerale().getFactureProforma() != null) {
            return facture.getFactureGenerale().getFactureProforma().getReferenceFacture();
        }
        return facture.getFactureProforma() != null
                ? facture.getFactureProforma().getReferenceFacture() : "—";
    }

    private static String designationLigne(LigneService ligne) {
        if (ligne == null) {
            return null;
        }
        String d = DemandeMapper.libelleLigne(ligne);
        if (d != null) {
            String nomService = ligne.getService() != null ? ligne.getService().getNomService() : null;
            boolean generique = "Prestation".equalsIgnoreCase(d) || "Abonnement mensuel".equalsIgnoreCase(d);
            if (generique && nomService != null && !nomService.isBlank()) {
                return d + " — " + nomService;
            }
            return d;
        }
        return ligne.getService() != null ? ligne.getService().getNomService() : "Service";
    }

    private static void detail(com.lowagie.text.Document doc, FactureValidee facture) {
        String designation = designationLigne(facture.getLigneService());

        com.lowagie.text.pdf.PdfPTable table =
                new com.lowagie.text.pdf.PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.4f});
        table.setSpacingBefore(10);

        com.lowagie.text.pdf.PdfPCell entete = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase("DÉTAIL DE LA FACTURE", PdfTheme.F_SECTION));
        entete.setColspan(2);
        entete.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        entete.setBorderColor(PdfTheme.PRIMAIRE);
        entete.setBorderWidth(1f);
        entete.setPaddingLeft(0);
        entete.setPaddingBottom(4);
        table.addCell(entete);

        com.lowagie.text.pdf.PdfPCell h1 = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase("Désignation", PdfTheme.F_LABEL));
        h1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        h1.setPadding(6);
        table.addCell(h1);
        com.lowagie.text.pdf.PdfPCell h2 = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase("Montant (Ariary)", PdfTheme.F_LABEL));
        h2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        h2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        h2.setPadding(6);
        table.addCell(h2);

        com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(PdfFormats.valeur(designation), PdfTheme.F_VALEUR));
        c1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c1.setPadding(7);
        table.addCell(c1);
        com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(
                        PdfFormats.formatMontant(facture.getMontant()), PdfTheme.F_VALEUR));
        c2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        c2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c2.setPadding(7);
        table.addCell(c2);

        com.lowagie.text.pdf.PdfPCell totalLibelle = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase("TOTAL À PAYER", PdfTheme.F_SECTION));
        totalLibelle.setBorder(com.lowagie.text.Rectangle.TOP);
        totalLibelle.setBorderColor(PdfTheme.PRIMAIRE);
        totalLibelle.setBorderWidth(1f);
        totalLibelle.setPadding(8);
        table.addCell(totalLibelle);
        com.lowagie.text.pdf.PdfPCell totalMontant = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(
                        PdfFormats.formatMontant(facture.getMontant()), PdfTheme.F_VALEUR));
        totalMontant.setBorder(com.lowagie.text.Rectangle.TOP);
        totalMontant.setBorderColor(PdfTheme.PRIMAIRE);
        totalMontant.setBorderWidth(1f);
        totalMontant.setPadding(8);
        totalMontant.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        table.addCell(totalMontant);

        doc.add(table);
    }

    private static void montantEnLettres(com.lowagie.text.Document doc, FactureValidee facture) {
        if (facture.getMontant() == null) {
            return;
        }
        doc.add(PdfComposants.encartLettres("Arrêtée la présente facture à la somme de ",
                MontantEnLettres.convertir(facture.getMontant().longValue())
                        .toUpperCase(Locale.FRANCE) + " ARIARY"));
    }

    private static void signatures(com.lowagie.text.Document doc, FactureValidee facture) {
        String emetteur = facture.getValidePar() != null
                ? PdfFormats.nomComplet(facture.getValidePar().getPrenom(),
                        facture.getValidePar().getNom())
                : null;

        com.lowagie.text.pdf.PdfPTable signatures = new com.lowagie.text.pdf.PdfPTable(2);
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
