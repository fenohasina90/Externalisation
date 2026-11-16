package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.entity.FactureValideeLigne;
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
import java.util.List;
import java.util.Locale;

/**
 * Facture validée mensuelle d'un abonnement : enfant de la facture générale validée.
 *
 * <p>Design calqué sur le reste du modèle CabEx : en-tête (badge « FACTURE
 * VALIDÉE »), meta-bloc (référence mensuelle, générale validée, proforma,
 * date demande, mois facturé, émission), tableau à 6 colonnes ; la désignation
 * de l'abonnement se place dans la case « Abonnement mensuel : » avec le nom du
 * service. Si la facture reprend des impayés des autres demandes (lignes RAPPEL),
 * une section dédiée « À titre de rappel de règlement » est affichée en évidence
 * — elle est informative et n'est pas comptabilisée dans le total de la facture.</p>
 */
final class PdfFactureAbonnement {

    private final ConfigurationService configurationService;

    PdfFactureAbonnement(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    byte[] generer(FactureValidee facture, BigDecimal avanceRecue) {
        byte[] pdf = null;
        BigDecimal avance = avanceRecue != null ? avanceRecue : BigDecimal.ZERO;
        for (float echelle : PdfStyleFacture.ECHELLES) {
            pdf = construire(facture, new PdfStyleFacture(echelle), avance);
            if (PdfDocument.nombrePages(pdf) <= 1) {
                break;
            }
        }
        return pdf;
    }

    private byte[] construire(FactureValidee facture, PdfStyleFacture st, BigDecimal avance) {
        List<FactureValideeLigne> lignes = lignesTriees(facture);
        String titreMeta = "Facture d'abonnement mensuel "
                + PdfFormats.valeur(facture.getReferenceFactureValidee());
        return PdfDocument.generer(PdfDocument.MiseEnPage.FACTURE,
                "Facture d'abonnement mensuel",
                titreMeta, doc -> ecrire(doc, facture, lignes, st, avance),
                "Erreur de génération de la facture d'abonnement");
    }

    private static List<FactureValideeLigne> lignesTriees(FactureValidee facture) {
        if (facture.getLignes() == null) {
            return List.of();
        }
        return facture.getLignes().stream()
                .sorted(java.util.Comparator.comparing(FactureValideeLigne::getId,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }

    private void ecrire(Document doc, FactureValidee facture,
                        List<FactureValideeLigne> lignes, PdfStyleFacture st,
                        BigDecimal avance) {
        DemandeService demande = facture.getDemande();
        Societe societe = demande != null ? demande.getSociete() : null;

        List<FactureValideeLigne> principales = lignes.stream()
                .filter(l -> "PRINCIPALE".equalsIgnoreCase(l.getType() != null ? l.getType() : "PRINCIPALE"))
                .toList();
        List<FactureValideeLigne> rappels = lignes.stream()
                .filter(l -> "RAPPEL".equalsIgnoreCase(l.getType() != null ? l.getType() : "RAPPEL"))
                .toList();

        entete(doc, facture, st);
        destinataire(doc, demande, societe, st);

        BigDecimal montant = PdfFormats.montantOuZero(facture.getMontant());
        doc.add(tableauObjet(facture, principales, montant, st));
        if (!rappels.isEmpty()) {
            doc.add(tableauRappels(facture, rappels, st));
        }

        BigDecimal totalHt = mainMontant(principales, montant);
        BigDecimal tauxTva = lireTauxTva();
        BigDecimal tva = totalHt.multiply(tauxTva)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal ttc = totalHt.add(tva);
        BigDecimal netAPayer = ttc.max(BigDecimal.ZERO);

        BigDecimal avanceEffective = avance.max(BigDecimal.ZERO).min(ttc);
        BigDecimal netAPayerRestant = ttc.subtract(avanceEffective).max(BigDecimal.ZERO);
        doc.add(blocBas(demande, st, totalHt, tauxTva, tva, ttc, avanceEffective, netAPayerRestant));
        doc.add(PdfFactureComposants.conditionsReglement(st));
        doc.add(signatures(st));
    }

    // ------------------------------------------------------------------
    // En-tête et destinataire
    // ------------------------------------------------------------------

    private static void entete(Document doc, FactureValidee facture, PdfStyleFacture st) {
        PdfComposants.enteteDocument(doc, st, "FACTURE", "VALIDÉE (abonnement mensuel)");

        FactureValidee generale = facture.getFactureGenerale();
        String referenceGenerale = generale != null
                ? generale.getReferenceFactureValidee()
                : (facture.getFactureProforma() != null ? "—" : null);
        String referenceProforma = generale != null && generale.getFactureProforma() != null
                ? generale.getFactureProforma().getReferenceFacture()
                : (facture.getFactureProforma() != null
                        ? facture.getFactureProforma().getReferenceFacture()
                        : null);
        DemandeService demande = facture.getDemande();

        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(st.ecart * 0.8f);
        meta.addCell(PdfComposants.faMeta(st, "N° FACTURE VALIDÉE",
                PdfFormats.valeur(facture.getReferenceFactureValidee()), PdfTheme.ROUGE));
        meta.addCell(PdfComposants.faMeta(st, "N° FACTURE GÉNÉRALE VALIDÉE",
                PdfFormats.valeur(referenceGenerale), PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "N° PROFORMA",
                PdfFormats.valeur(referenceProforma), PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "DATE DEMANDE",
                PdfFormats.valeurDate(demande != null ? demande.getDateDemande() : null),
                PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "MOIS FACTURÉ",
                moisLabel(facture.getMois()), PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "DATE D'ÉMISSION",
                facture.getValideLe() != null
                        ? PdfFormats.valeurDate(facture.getValideLe().toLocalDate()) : null,
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
    // Tableau principal — Abonnement mensuel
    // ------------------------------------------------------------------

    private static PdfPTable tableauObjet(FactureValidee facture,
                                          List<FactureValideeLigne> principales,
                                          BigDecimal montant, PdfStyleFacture st) {
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

        LigneService ligne = facture.getLigneService();
        String designation = designationAbonnement(ligne);
        BigDecimal pu = principales.stream().map(FactureValideeLigne::getMontant)
                .map(m -> m != null ? m : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO);
        if (pu.signum() == 0) {
            pu = montant;
        }

        PdfPCell bande = PdfComposants.faCellule(st,
                new Phrase("Abonnement mensuel :", st.section), Element.ALIGN_LEFT);
        bande.setColspan(6);
        bande.setBackgroundColor(PdfTheme.VERT_PALE);
        bande.setBorder(Rectangle.LEFT);
        bande.setBorderWidthLeft(2.5f);
        bande.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        table.addCell(bande);

        Phrase designationPhrase = new Phrase();
        designationPhrase.setLeading(st.td.getSize() * 1.3f);
        designationPhrase.add(new Chunk(PdfFormats.valeur(designation), st.tdGras));
        PdfComposants.faCorps(table, st, designationPhrase, Element.ALIGN_LEFT, false);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(pu), st.td),
                Element.ALIGN_RIGHT, false);
        PdfComposants.faCorps(table, st, new Phrase("1", st.td), Element.ALIGN_CENTER, false);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(pu), st.td),
                Element.ALIGN_RIGHT, false);
        PdfComposants.faCorps(table, st, new Phrase("-", st.td), Element.ALIGN_RIGHT, false);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(pu), st.td),
                Element.ALIGN_RIGHT, false);

        PdfComposants.faSousTotal(table, st, "Sous-total Abonnement mensuel", 3);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(pu), 1);
        PdfComposants.faSousTotal(table, st, "-", 1);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(pu), 1);

        PdfComposants.faSousTotal(table, st, "TOTAL HT", 3);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(pu), 1);
        PdfComposants.faSousTotal(table, st, "-", 1);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(pu), 1);
        return table;
    }

    private static PdfPTable tableauRappels(FactureValidee facture,
                                            List<FactureValideeLigne> rappels,
                                            PdfStyleFacture st) {
        BigDecimal total = rappels.stream()
                .map(l -> l.getMontant() != null ? l.getMontant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.2f, 1.6f, 1.1f, 1.1f});
        table.setSpacingBefore(st.ecart);
        table.setHeaderRows(2);

        PdfPCell bande = PdfComposants.faCellule(st,
                new Phrase("À titre de rappel de règlement",
                        com.lowagie.text.FontFactory.getFont(
                                "Helvetica", 9f, com.lowagie.text.Font.BOLD,
                                new java.awt.Color(180, 83, 9))),
                Element.ALIGN_LEFT);
        bande.setColspan(4);
        bande.setBackgroundColor(new java.awt.Color(254, 243, 199));
        bande.setBorder(Rectangle.LEFT);
        bande.setBorderWidthLeft(2.5f);
        bande.setBorderColorLeft(new java.awt.Color(180, 83, 9));
        table.addCell(bande);

        PdfPCell explication = new PdfPCell(new Paragraph(
                "Les factures validées ci-dessous restant impayées ne sont pas comptabilisées "
                        + "dans la présente facture — chacune se règle sur sa propre facture validée.",
                PdfTheme.F_MENTION));
        explication.setColspan(4);
        explication.setPadding(6);
        explication.setBackgroundColor(new java.awt.Color(255, 255, 255));
        explication.setBorderWidth(0);
        table.addCell(explication);

        PdfComposants.faTh(table, st, "Désignation", Element.ALIGN_LEFT);
        PdfComposants.faTh(table, st, "Demande", Element.ALIGN_LEFT);
        PdfComposants.faTh(table, st, "Reste (Ariary)", Element.ALIGN_RIGHT);
        PdfComposants.faTh(table, st, "Montant", Element.ALIGN_CENTER);

        for (FactureValideeLigne ligne : rappels) {
            String refDemande = ligne.getLigneService() != null && ligne.getLigneService().getDemande() != null
                    ? ligne.getLigneService().getDemande().getReferenceDemande()
                    : null;
            PdfComposants.faCorps(table, st, new Phrase(PdfFormats.valeur(ligne.getDesignation()), st.td),
                    Element.ALIGN_LEFT, false);
            PdfComposants.faCorps(table, st, new Phrase(PdfFormats.valeur(refDemande), st.td),
                    Element.ALIGN_LEFT, false);
            PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(ligne.getMontant()), st.td),
                    Element.ALIGN_RIGHT, false);
            PdfComposants.faCorps(table, st, new Phrase("Paiement sur facture validée", st.petit),
                    Element.ALIGN_CENTER, false);
        }

        PdfComposants.faSousTotal(table, st, "Total des règlements rappelés (non inclus)", 2);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(total), 1);
        PdfComposants.faSousTotal(table, st, "", 1);
        return table;
    }

    private static BigDecimal mainMontant(List<FactureValideeLigne> principales, BigDecimal fallback) {
        BigDecimal total = principales.stream()
                .map(l -> l.getMontant() != null ? l.getMontant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.signum() == 0 ? fallback : total;
    }

    private static String designationAbonnement(LigneService ligne) {
        if (ligne == null) {
            return "Abonnement";
        }
        String nomService = ligne.getService() != null ? ligne.getService().getNomService() : null;
        return nomService != null && !nomService.isBlank() ? nomService.trim()
                : com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(ligne);
    }

    private static String moisLabel(java.time.LocalDate mois) {
        if (mois == null) {
            return null;
        }
        String s = java.time.format.DateTimeFormatter
                .ofPattern("MMMM yyyy", Locale.FRANCE).format(mois);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------
    // Totaux, montant en lettres et signatures
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

    private static PdfPTable signatures(PdfStyleFacture st) {
        PdfPTable signatures = PdfComposants.signaturesStyle(st, st.hSignature,
                new String[]{"Client"},
                new String[]{"Responsable Traitement", "Dossiers Clients"},
                new String[]{"Responsable Audit", "Contrôleur de Gestion"},
                new String[]{"Manager"});
        signatures.setSpacingBefore(st.ecart);
        return signatures;
    }

    private BigDecimal lireTauxTva() {
        try {
            String valeur = configurationService.lireValeur("TAUX_TVA", "0");
            return new BigDecimal(valeur.trim()).max(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
