package com.cabex.backend.document.service;

import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;

/**
 * Plan de paiement (échéancier d'une demande).
 */
final class PdfPlanPaiement {

    private PdfPlanPaiement() {
    }

    static byte[] generer(ProposPaiement propos) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT, "Plan de paiement",
                "Plan de paiement",
                doc -> ecrire(doc, propos),
                "Erreur de génération du plan de paiement");
    }

    private static void ecrire(Document doc, ProposPaiement propos) {
        String reference = propos.getDemande() != null
                ? propos.getDemande().getReferenceDemande() : null;
        PdfComposants.titreDocument(doc, "Plan de paiement",
                "Demande : " + PdfFormats.valeur(reference));

        BigDecimal totalAttendu = total(propos, true);
        BigDecimal totalPaye = total(propos, false);

        PdfComposants.rangee(doc, 0.4f,
                PdfComposants.kpi("MONTANT TOTAL", PdfFormats.formatArgent(propos.getMontantTotal()), false),
                PdfComposants.kpi("DÉJÀ PAYÉ", PdfFormats.formatArgent(totalPaye), false),
                PdfComposants.kpi("RESTE À PAYER", PdfFormats.formatArgent(propos.getRestePayer()), true));

        cartes(doc, propos, reference);
        echeancier(doc, propos, totalAttendu, totalPaye);
    }

    private static void cartes(Document doc, ProposPaiement propos, String reference) {
        PdfPCell cDemande = PdfComposants.carte("Demande", new String[][]{
                {"Référence", PdfFormats.valeur(reference)},
                {"Nombre d'échéances", propos.getNombreEcheances() != null
                        ? propos.getNombreEcheances().toString() : "-"}
        });
        PdfPCell cPeriode = PdfComposants.carte("Période", new String[][]{
                {"Début", PdfFormats.valeurDate(propos.getDateDebut())},
                {"Fin", PdfFormats.valeurDate(propos.getDateFin())}
        });
        PdfComposants.rangee(doc, 0.5f, cDemande, cPeriode);
    }

    private static BigDecimal total(ProposPaiement propos, boolean attendu) {
        BigDecimal total = BigDecimal.ZERO;
        if (propos.getEcheances() == null) {
            return total;
        }
        for (EcheancePaiement echeance : propos.getEcheances()) {
            total = total.add(PdfFormats.montantOuZero(attendu
                    ? echeance.getMontantAttendu() : echeance.getMontantPaye()));
        }
        return total;
    }

    private static void echeancier(Document doc, ProposPaiement propos,
                                   BigDecimal totalAttendu, BigDecimal totalPaye) {
        PdfComposants.section(doc, "Échéancier");
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.1f, 2.4f, 3f, 3f, 3f});
        table.setHeaderRows(1);
        PdfComposants.entete(table, "N°", Element.ALIGN_CENTER);
        PdfComposants.entete(table, "DATE D'ÉCHÉANCE", Element.ALIGN_LEFT);
        PdfComposants.entete(table, "MONTANT ATTENDU", Element.ALIGN_RIGHT);
        PdfComposants.entete(table, "MONTANT PAYÉ", Element.ALIGN_RIGHT);
        PdfComposants.entete(table, "RESTE", Element.ALIGN_RIGHT);

        if (propos.getEcheances() == null || propos.getEcheances().isEmpty()) {
            ligneVide(table);
        } else {
            lignes(table, propos);
            ligneTotaux(table, totalAttendu, totalPaye);
        }
        doc.add(table);
    }

    private static void lignes(PdfPTable table, ProposPaiement propos) {
        int index = 0;
        for (EcheancePaiement echeance : propos.getEcheances()) {
            boolean zebra = index++ % 2 == 1;
            BigDecimal reste = PdfFormats.montantOuZero(echeance.getMontantAttendu())
                    .subtract(PdfFormats.montantOuZero(echeance.getMontantPaye()))
                    .max(BigDecimal.ZERO);
            PdfComposants.corps(table, echeance.getNumeroEcheance() != null
                    ? echeance.getNumeroEcheance().toString() : "-", Element.ALIGN_CENTER, zebra);
            PdfComposants.corps(table, PdfFormats.valeurDate(echeance.getDateEcheance()),
                    Element.ALIGN_LEFT, zebra);
            PdfComposants.corps(table, PdfFormats.formatArgent(echeance.getMontantAttendu()),
                    Element.ALIGN_RIGHT, zebra);
            PdfComposants.corps(table, PdfFormats.formatArgent(echeance.getMontantPaye()),
                    Element.ALIGN_RIGHT, zebra);
            PdfComposants.corps(table, PdfFormats.formatArgent(reste), Element.ALIGN_RIGHT, zebra);
        }
    }

    private static void ligneTotaux(PdfPTable table, BigDecimal totalAttendu, BigDecimal totalPaye) {
        PdfComposants.celluleTotal(table, "TOTAL", Element.ALIGN_RIGHT, 2);
        PdfComposants.celluleTotal(table, PdfFormats.formatArgent(totalAttendu),
                Element.ALIGN_RIGHT, 1);
        PdfComposants.celluleTotal(table, PdfFormats.formatArgent(totalPaye),
                Element.ALIGN_RIGHT, 1);
        PdfComposants.celluleTotal(table, PdfFormats.formatArgent(
                        totalAttendu.subtract(totalPaye).max(BigDecimal.ZERO)),
                Element.ALIGN_RIGHT, 1);
    }

    private static void ligneVide(PdfPTable table) {
        PdfPCell vide = PdfComposants.base(
                new Phrase("Aucune échéance enregistrée", PdfTheme.F_PETIT_ITALIQUE),
                Element.ALIGN_CENTER);
        vide.setColspan(5);
        vide.setBorder(Rectangle.BOTTOM);
        vide.setBorderColor(PdfTheme.GRIS_BORDURE);
        table.addCell(vide);
    }
}
