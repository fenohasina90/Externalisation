package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Reçu de paiement.
 */
final class PdfRecuPaiement {

    private PdfRecuPaiement() {
    }

    static byte[] generer(Paiement paiement) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT, "Reçu de paiement",
                "Reçu de paiement " + PdfFormats.valeur(paiement.getReferencePaiement()),
                doc -> ecrire(doc, paiement),
                "Erreur de génération du reçu de paiement");
    }

    private static void ecrire(Document doc, Paiement paiement) {
        PdfComposants.titreDocument(doc, "Reçu de paiement",
                "Référence paiement : " + PdfFormats.valeur(paiement.getReferencePaiement()));
        doc.add(bandeauMontant(paiement));
        cartes(doc, paiement);
        montantEnLettres(doc, paiement);
        signatures(doc, paiement);
    }

    private static PdfPTable bandeauMontant(Paiement paiement) {
        PdfPCell montant = new PdfPCell();
        montant.setBackgroundColor(PdfTheme.PRIMAIRE);
        montant.setBorder(Rectangle.NO_BORDER);
        montant.setPadding(16);

        Paragraph libelle = new Paragraph("MONTANT REÇU", PdfTheme.F_LABEL_BLANC);
        libelle.setAlignment(Element.ALIGN_CENTER);
        montant.addElement(libelle);

        Paragraph valeur = new Paragraph(PdfFormats.formatArgent(paiement.getMontantPaye()),
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

    private static void cartes(Document doc, Paiement paiement) {
        String client = paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                ? paiement.getDemande().getSociete().getNomSociete() : null;

        EcheancePaiement echeance = paiement.getEcheance();
        LigneService ligne = echeance != null && echeance.getProposPaiement() != null
                ? echeance.getProposPaiement().getLigneService() : null;
        String service = ligne != null && ligne.getService() != null
                ? ligne.getService().getNomService() : null;
        String libelleEcheance = echeance != null
                ? "N° " + echeance.getNumeroEcheance() + " — "
                        + PdfFormats.valeurDate(echeance.getDateEcheance())
                : null;

        PdfPCell cClient = PdfComposants.carte("Client et demande", new String[][]{
                {"Client", PdfFormats.valeur(client)},
                {"Demande", PdfFormats.valeur(paiement.getDemande() != null
                        ? paiement.getDemande().getReferenceDemande() : null)}
        });
        PdfPCell cService = PdfComposants.carte("Service et échéance", new String[][]{
                {"Service", PdfFormats.valeur(service)},
                {"Échéance", PdfFormats.valeur(libelleEcheance)}
        });
        PdfPCell cReglement = PdfComposants.carte("Règlement", new String[][]{
                {"Date de paiement", PdfFormats.valeurDate(paiement.getDatePaiement())},
                {"Mode de paiement", PdfFormats.valeur(paiement.getModePaiement() != null
                        ? paiement.getModePaiement().getLibelle() : null)},
                {"Référence virement", PdfFormats.valeur(paiement.getReferenceVirement())}
        });
        PdfPCell cMontants = PdfComposants.carte("Montants de l'échéance", new String[][]{
                {"Montant attendu", PdfFormats.formatArgent(montantAttendu(echeance))},
                {"Montant payé", PdfFormats.formatArgent(montantPaye(echeance, paiement))},
                {"Reste à payer", PdfFormats.formatArgent(reste(echeance, paiement))}
        });

        PdfComposants.rangee(doc, 0.5f, cClient, cService);
        PdfComposants.rangee(doc, 0.5f, cReglement, cMontants);
    }

    private static BigDecimal montantAttendu(EcheancePaiement echeance) {
        return echeance != null ? echeance.getMontantAttendu() : null;
    }

    private static BigDecimal montantPaye(EcheancePaiement echeance, Paiement paiement) {
        return echeance != null ? echeance.getMontantPaye() : paiement.getMontantPaye();
    }

    private static BigDecimal reste(EcheancePaiement echeance, Paiement paiement) {
        BigDecimal attendu = montantAttendu(echeance);
        if (attendu == null) {
            return null;
        }
        return attendu.subtract(PdfFormats.montantOuZero(montantPaye(echeance, paiement)));
    }

    private static void montantEnLettres(Document doc, Paiement paiement) {
        if (paiement.getMontantPaye() == null) {
            return;
        }
        doc.add(PdfComposants.encartLettres("Arrêté le présent reçu à la somme de ",
                MontantEnLettres.convertir(paiement.getMontantPaye().longValue())
                        .toUpperCase(Locale.FRANCE) + " ARIARY"));
    }

    private static void signatures(Document doc, Paiement paiement) {
        String enregistrePar = paiement.getCreatedBy() != null
                ? PdfFormats.nomComplet(paiement.getCreatedBy().getPrenom(),
                        paiement.getCreatedBy().getNom())
                : null;

        PdfPTable signatures = new PdfPTable(2);
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(26);
        signatures.setKeepTogether(true);
        signatures.setWidths(new float[]{1f, 1f});
        signatures.addCell(PdfComposants.celluleSignature("Le client", null));
        signatures.addCell(PdfComposants.celluleSignature("Pour le cabinet",
                enregistrePar != null ? "Enregistré par " + enregistrePar : null));
        signatures.addCell(PdfComposants.celluleEspaceSignature(70));
        signatures.addCell(PdfComposants.celluleEspaceSignature(70));
        doc.add(signatures);
    }
}
