package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.facture.entity.Facture;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Facture proforma au format du modèle CABEX, toujours générée sur une seule page.
 */
final class PdfFactureProforma {

    private final ConfigurationService configurationService;

    PdfFactureProforma(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    byte[] generer(Facture facture) {
        byte[] pdf = null;
        for (float echelle : PdfStyleFacture.ECHELLES) {
            pdf = construire(facture, new PdfStyleFacture(echelle));
            if (PdfDocument.nombrePages(pdf) <= 1) {
                break;
            }
        }
        return pdf;
    }

    private byte[] construire(Facture facture, PdfStyleFacture st) {
        String titreMeta = "Facture proforma " + PdfFormats.valeur(facture.getReferenceFacture());
        return PdfDocument.generer(PdfDocument.MiseEnPage.FACTURE, "Facture proforma",
                titreMeta, doc -> ecrire(doc, facture, st),
                "Erreur de génération de la facture proforma");
    }

    private void ecrire(Document doc, Facture facture, PdfStyleFacture st) {
        DemandeService demande = facture.getDemande();
        Societe societe = demande != null ? demande.getSociete() : null;

        entete(doc, facture, st);
        destinataire(doc, demande, societe, st);

        BigDecimal totalHt = totalHt(facture);
        doc.add(tableauPrestations(facture, st, totalHt));

        BigDecimal tauxTva = lireTauxTva();
        BigDecimal tva = totalHt.multiply(tauxTva)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal ttc = totalHt.add(tva);
        BigDecimal avance = avanceRecue(facture);
        BigDecimal netAPayer = ttc.subtract(avance).max(BigDecimal.ZERO);

        doc.add(blocBas(demande, st, totalHt, tauxTva, tva, ttc, avance, netAPayer));
        doc.add(PdfFactureComposants.conditionsReglement(st));
        doc.add(signatures(st));
    }

    // ------------------------------------------------------------------
    // En-tête et destinataire
    // ------------------------------------------------------------------

    private static void entete(Document doc, Facture facture, PdfStyleFacture st) {
        PdfComposants.enteteDocument(doc, st, "FACTURE", "PROFORMA");

        DemandeService demande = facture.getDemande();
        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(st.ecart * 0.8f);
        meta.addCell(PdfComposants.faMeta(st, "N° FACTURE",
                PdfFormats.valeur(facture.getReferenceFacture()), PdfTheme.ROUGE));
        meta.addCell(PdfComposants.faMeta(st, "DATE",
                PdfFormats.valeurDate(facture.getCreatedAt() != null
                        ? facture.getCreatedAt().toLocalDate() : null), PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "DEMANDE",
                PdfFormats.valeur(demande != null ? demande.getReferenceDemande() : null),
                PdfTheme.PRIMAIRE));
        doc.add(meta);
    }

    private static void destinataire(Document doc, DemandeService demande, Societe societe,
                                     PdfStyleFacture st) {
        PdfPCell cDest = PdfComposants.faCarte(st, "Facturé à", new String[][]{
                {"Raison sociale", PdfFormats.valeur(societe != null ? societe.getNomSociete() : null)},
                {"Adresse", PdfFormats.valeur(societe != null ? societe.getAdresse() : null)},
                {"Contact", PdfFormats.valeur(societe != null ? societe.getContact() : null)},
                {"Mail", PdfFormats.valeur(societe != null ? societe.getEmail() : null)},
                {"NIF / STAT / RCS", "..................  /  ..................  /  .................."}
        });
        PdfPCell cRef = PdfComposants.faCarte(st, "Informations", new String[][]{
                {"Responsable", PdfFormats.valeur(demande != null
                        && demande.getClientResponsable() != null
                        ? demande.getClientResponsable().getNomClient() : null)},
                {"Demande", PdfFormats.valeur(demande != null ? demande.getReferenceDemande() : null)},
                {"Règlement", "À payer dès la réception"}
        });
        PdfComposants.rangee(doc, 0.3f, cDest, cRef);
    }

    // ------------------------------------------------------------------
    // Tableau des prestations
    // ------------------------------------------------------------------

    private static PdfPTable tableauPrestations(Facture facture, PdfStyleFacture st,
                                                BigDecimal totalHt) {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.4f, 1.5f, 0.7f, 1.9f, 1.3f, 1.9f});
        table.setHeaderRows(1);
        table.setSpacingBefore(st.ecart);
        PdfComposants.faTh(table, st, "DÉSIGNATION", Element.ALIGN_LEFT);
        PdfComposants.faTh(table, st, "PU\n(Ariary)", Element.ALIGN_RIGHT);
        PdfComposants.faTh(table, st, "QTÉ", Element.ALIGN_CENTER);
        PdfComposants.faTh(table, st, "MONTANT HTVA\n(Ariary)", Element.ALIGN_RIGHT);
        PdfComposants.faTh(table, st, "REMISE\n(Ariary)", Element.ALIGN_RIGHT);
        PdfComposants.faTh(table, st, "NET COMMERCIAL\n(Ariary)", Element.ALIGN_RIGHT);

        PdfPCell bande = PdfComposants.faCellule(st,
                new Phrase("PRESTATIONS", st.section), Element.ALIGN_LEFT);
        bande.setColspan(6);
        bande.setBackgroundColor(PdfTheme.VERT_PALE);
        bande.setBorder(Rectangle.LEFT);
        bande.setBorderWidthLeft(2.5f);
        bande.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        table.addCell(bande);

        if (facture.getLignes() != null) {
            int index = 0;
            for (LigneService ligne : facture.getLignes()) {
                lignesPrestation(table, st, ligne, index++ % 2 == 1);
            }
        }

        PdfComposants.faSousTotal(table, st, "Sous-total Prestation", 3);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(totalHt), 1);
        PdfComposants.faSousTotal(table, st, "-", 1);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(totalHt), 1);
        return table;
    }

    private static void lignesPrestation(PdfPTable table, PdfStyleFacture st,
                                         LigneService ligne, boolean zebra) {
        Phrase designation = new Phrase();
        designation.setLeading(st.td.getSize() * 1.3f);
        designation.add(new Chunk(PdfFormats.valeur(ligne.getService() != null
                ? ligne.getService().getNomService() : null), st.tdGras));
        PdfComposants.faCorps(table, st, designation, Element.ALIGN_LEFT, zebra);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(ligne.getTarif()), st.td),
                Element.ALIGN_RIGHT, zebra);
        PdfComposants.faCorps(table, st, new Phrase("1", st.td), Element.ALIGN_CENTER, zebra);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(ligne.getTarif()), st.td),
                Element.ALIGN_RIGHT, zebra);
        PdfComposants.faCorps(table, st, new Phrase("-", st.td), Element.ALIGN_RIGHT, zebra);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(ligne.getTarif()), st.td),
                Element.ALIGN_RIGHT, zebra);
    }

    private static BigDecimal totalHt(Facture facture) {
        BigDecimal total = BigDecimal.ZERO;
        if (facture.getLignes() == null) {
            return total;
        }
        for (LigneService ligne : facture.getLignes()) {
            total = total.add(PdfFormats.montantOuZero(ligne.getTarif()));
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Totaux, montant en lettres et observations
    // ------------------------------------------------------------------

    private static PdfPTable blocBas(DemandeService demande, PdfStyleFacture st,
                                     BigDecimal totalHt, BigDecimal tauxTva, BigDecimal tva,
                                     BigDecimal ttc, BigDecimal avance, BigDecimal netAPayer) {
        PdfPTable gauche = new PdfPTable(1);
        gauche.setWidthPercentage(100);
        gauche.addCell(PdfComposants.celluleLettres("Arrêtée la présente facture à la somme de",
                MontantEnLettres.convertir(netAPayer.longValue()).toUpperCase(Locale.FRANCE)
                        + " ARIARY"));
        PdfPCell espace = PdfComposants.celluleVide();
        espace.setFixedHeight(st.ecart * 0.6f);
        gauche.addCell(espace);
        gauche.addCell(observations(demande, st));

        PdfPTable totaux = new PdfPTable(2);
        totaux.setWidthPercentage(100);
        totaux.setWidths(new float[]{5f, 4.6f});
        PdfComposants.faLigneTotaux(totaux, st, "TOTAL HT",
                PdfFormats.formatMontant(totalHt), 0);
        PdfComposants.faLigneTotaux(totaux, st, "TVA" + (tauxTva.signum() > 0
                        ? " (" + tauxTva.stripTrailingZeros().toPlainString() + " %)" : ""),
                PdfFormats.formatMontant(tva), 0);
        PdfComposants.faLigneTotaux(totaux, st, "TOTAL TTC",
                PdfFormats.formatMontant(ttc), 1);
        PdfComposants.faLigneTotaux(totaux, st, "Avance reçue",
                PdfFormats.formatMontant(avance), 0);
        PdfComposants.faLigneTotaux(totaux, st, "NET À PAYER",
                PdfFormats.formatMontant(netAPayer), 2);

        PdfPTable bas = new PdfPTable(3);
        bas.setWidthPercentage(100);
        bas.setWidths(new float[]{5.4f, 0.35f, 4.25f});
        bas.setSpacingBefore(st.ecart);
        bas.setKeepTogether(true);
        bas.addCell(celluleSansBordure(gauche));
        bas.addCell(PdfComposants.celluleVide());
        bas.addCell(celluleSansBordure(totaux));
        return bas;
    }

    private static PdfPCell observations(DemandeService demande, PdfStyleFacture st) {
        PdfPCell obs = new PdfPCell();
        obs.setBorderColor(PdfTheme.GRIS_BORDURE);
        obs.setBorderWidth(0.5f);
        obs.setPadding(st.padCarte);
        obs.setMinimumHeight(st.hObservations);
        obs.addElement(new Paragraph("OBSERVATIONS", st.carteTitre));
        String commentaire = demande != null ? PdfFormats.valeurTexte(demande.getCommentaire()) : "";
        if (!commentaire.isEmpty()) {
            Paragraph p = new Paragraph(commentaire, st.petit);
            p.setSpacingBefore(2);
            obs.addElement(p);
        }
        return obs;
    }

    private static PdfPCell celluleSansBordure(PdfPTable contenu) {
        PdfPCell c = new PdfPCell(contenu);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    // ------------------------------------------------------------------
    // Signatures
    // ------------------------------------------------------------------

    private static PdfPTable signatures(PdfStyleFacture st) {
        PdfPTable signatures = PdfComposants.signaturesStyle(st, st.hSignature,
                new String[]{"Client"},
                new String[]{"Responsable Traitement", "Dossiers Clients"},
                new String[]{"Responsable Audit", "Contrôleur de Gestion"},
                new String[]{"Manager"});
        signatures.setSpacingBefore(st.ecart);
        return signatures;
    }

    // ------------------------------------------------------------------
    // Calculs
    // ------------------------------------------------------------------

    private BigDecimal lireTauxTva() {
        try {
            String valeur = configurationService.lireValeur("TAUX_TVA", "0");
            return new BigDecimal(valeur.trim()).max(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** Avance déjà reçue : somme payée sur les échéances des lignes facturées. */
    private static BigDecimal avanceRecue(Facture facture) {
        BigDecimal avance = BigDecimal.ZERO;
        if (facture.getLignes() == null) {
            return avance;
        }
        for (LigneService ligne : facture.getLignes()) {
            if (ligne.getPlansPaiement() == null) {
                continue;
            }
            for (ProposPaiement plan : ligne.getPlansPaiement()) {
                if (plan.getEcheances() == null) {
                    continue;
                }
                for (EcheancePaiement echeance : plan.getEcheances()) {
                    avance = avance.add(PdfFormats.montantOuZero(echeance.getMontantPaye()));
                }
            }
        }
        return avance;
    }
}
