package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.mapper.DemandeMapper;
import com.cabex.backend.facture.entity.FactureValidee;
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
        return generer(paiement, null);
    }

    static byte[] generer(Paiement paiement, BigDecimal sommePayeeSurFacture) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT, "Reçu de paiement",
                "Reçu de paiement " + PdfFormats.valeur(paiement.getReferencePaiement()),
                doc -> ecrire(doc, paiement, sommePayeeSurFacture),
                "Erreur de génération du reçu de paiement");
    }

    private static void ecrire(Document doc, Paiement paiement, BigDecimal sommePayeeSurFacture) {
        PdfComposants.titreDocument(doc, "Reçu de paiement",
                "Référence paiement : " + PdfFormats.valeur(paiement.getReferencePaiement()));
        doc.add(bandeauMontant(paiement));
        cartes(doc, paiement, sommePayeeSurFacture);
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

    private static void cartes(Document doc, Paiement paiement, BigDecimal sommePayeeSurFacture) {
        String client = paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                ? paiement.getDemande().getSociete().getNomSociete() : null;

        com.cabex.backend.facture.entity.FactureValidee fv = paiement.getFactureValidee();
        LigneService ligne = fv != null && fv.getFactureProforma() != null
                ? fv.getFactureProforma().getLignes().stream()
                        .filter(l -> l.getDemande() != null
                                && l.getDemande().getId().equals(paiement.getDemande().getId()))
                        .findFirst().orElse(null)
                : null;
        String service = ligne != null
                ? com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(ligne) : null;
        String libelleFacture = fv != null
                ? fv.getReferenceFactureValidee() != null
                        ? fv.getReferenceFactureValidee()
                        : fv.getFactureProforma() != null
                                ? fv.getFactureProforma().getReferenceFacture()
                                : null
                : null;

        PdfPCell cClient = PdfComposants.carte("Client et demande", new String[][]{
                {"Client", PdfFormats.valeur(client)},
                {"Demande", PdfFormats.valeur(paiement.getDemande() != null
                        ? paiement.getDemande().getReferenceDemande() : null)}
        });
        PdfPCell cService = PdfComposants.carte("Service et facture", new String[][]{
                {"Service", PdfFormats.valeur(service)},
                {"Facture validée", PdfFormats.valeur(libelleFacture)}
        });
        PdfPCell cReglement = PdfComposants.carte("Règlement", new String[][]{
                {"Date de paiement", PdfFormats.valeurDate(paiement.getDatePaiement())},
                {"Mode de paiement", PdfFormats.valeur(paiement.getModePaiement() != null
                        ? paiement.getModePaiement().getLibelle() : null)},
                {"Référence virement", PdfFormats.valeur(paiement.getReferenceVirement())}
        });
        PdfPCell cMontants = PdfComposants.carte("Montants de la facture", new String[][]{
                {"Montant facture", PdfFormats.formatArgent(montantFacture(fv))},
                {"Paiement du reçu", PdfFormats.formatArgent(montantPaye(paiement))},
                {"Total payé sur la facture", PdfFormats.formatArgent(sommePayeeSurFacture(paiement, sommePayeeSurFacture))},
                {"Reste à payer", PdfFormats.formatArgent(reste(fv, paiement, sommePayeeSurFacture))}
        });

        PdfComposants.rangee(doc, 0.5f, cClient, cService);
        PdfComposants.rangee(doc, 0.5f, cReglement, cMontants);
    }

    private static BigDecimal montantFacture(com.cabex.backend.facture.entity.FactureValidee fv) {
        return fv != null ? fv.getMontant() : null;
    }

    private static BigDecimal montantPaye(Paiement paiement) {
        return paiement.getMontantPaye();
    }

    private static BigDecimal sommePayeeSurFacture(Paiement paiement, BigDecimal somme) {
        return somme != null ? somme : paiement.getMontantPaye();
    }

    private static BigDecimal reste(com.cabex.backend.facture.entity.FactureValidee fv,
                                    Paiement paiement, BigDecimal sommePayeeSurFacture) {
        BigDecimal attendu = montantFacture(fv);
        if (attendu == null) {
            return null;
        }
        return attendu.subtract(
                        PdfFormats.montantOuZero(sommePayeeSurFacture(paiement, sommePayeeSurFacture)))
                .max(BigDecimal.ZERO);
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
