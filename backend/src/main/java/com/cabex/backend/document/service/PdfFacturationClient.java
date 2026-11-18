package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.facture.dto.FactureValideeFicheResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Facturation d'un client au format PDF (onglet « Facturation » de la fiche client).
 *
 * <p>Toutes les factures validées liées au client, rendues une par une, chacune
 * avec les paiements qui lui sont liés. La désignation de chaque facture liste
 * tous les services facturés avec leur type (Prestation / Abonnement / Débours,
 * mois en fin pour l'abonnement). Le filtre de dates porte sur la date de chaque
 * facture, l'ordre des dates (croissant / décroissant) et la recherche libre.</p>
 */
final class PdfFacturationClient {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font F_ROUGE = PdfTheme.font(8, Font.BOLD, PdfTheme.ROUGE);
    private static final Font F_TH = PdfTheme.font(7, Font.BOLD, Color.WHITE);
    private static final Font F_TD = PdfTheme.font(8, Font.NORMAL, PdfTheme.TEXTE);
    private static final Font F_TD_GRAS = PdfTheme.font(8, Font.BOLD, PdfTheme.TEXTE);
    private static final Font F_TD_ITALIQUE = PdfTheme.font(7, Font.ITALIC, PdfTheme.GRIS_TEXTE);
    private static final Font F_PREST = PdfTheme.font(8, Font.BOLD, new java.awt.Color(0x1E4F8F));
    private static final Font F_DEB = PdfTheme.font(8, Font.BOLD, new java.awt.Color(0x8A5200));
    private static final Font F_ABO = PdfTheme.font(8, Font.BOLD, new java.awt.Color(0x1F6B3F));
    private static final Font F_REGL = PdfTheme.font(7, Font.BOLD, new java.awt.Color(0x1F6B3F));

    private PdfFacturationClient() {
    }

    static byte[] generer(Societe societe,
                          List<FactureValideeFicheResponse> fiches,
                          LocalDate debut, LocalDate fin, String ordre, String recherche) {
        boolean ordreAsc = "ASC".equalsIgnoreCase(ordre);
        List<FactureValideeFicheResponse> liste = filtrer(fiches, debut, fin, ordreAsc, recherche);
        String client = societe != null && societe.getNomSociete() != null
                ? societe.getNomSociete() : "—";
        int idClient = societe != null && societe.getId() != null ? societe.getId() : 0;
        String enteteLigne = client + "  ·  " + codeSociete(idClient);
        return PdfDocument.generer(new com.lowagie.text.Rectangle(842, 595),
                PdfDocument.MiseEnPage.FACTURATION, enteteLigne,
                "Facturation client — " + client,
                doc -> ecrire(doc, liste, debut, fin, ordreAsc),
                "Erreur de génération du PDF de facturation client");
    }

    // ------------------------------------------------------------------
    // Filtre (mêmes règles que le frontend)
    // ------------------------------------------------------------------

    private static List<FactureValideeFicheResponse> filtrer(List<FactureValideeFicheResponse> fiches,
                                                             LocalDate debut, LocalDate fin,
                                                             boolean asc, String recherche) {
        Comparator<LocalDate> cmp = asc
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());
        String terme = recherche != null ? recherche.trim().toLowerCase() : "";
        java.util.function.Predicate<FactureValideeFicheResponse> matchRecherche = f -> {
            if (terme.isEmpty()) {
                return true;
            }
            StringBuilder hay = new StringBuilder();
            hay.append(f.referenceFactureValidee() != null ? f.referenceFactureValidee() : "").append(' ');
            hay.append(f.referenceDemande() != null ? f.referenceDemande() : "").append(' ');
            f.objets().forEach(o -> hay.append(o.libelle() != null ? o.libelle() : "").append(' '));
            f.paiements().forEach(p ->
                hay.append(p.referencePaiement() != null ? p.referencePaiement() : "").append(' '));
            return hay.toString().toLowerCase().contains(terme);
        };
        return fiches.stream()
                .filter(f -> inRange(f.dateFactureValidee(), debut, fin))
                .filter(matchRecherche)
                .sorted(Comparator.comparing(FactureValideeFicheResponse::dateFactureValidee, cmp)
                        .thenComparing(FactureValideeFicheResponse::id,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static boolean inRange(LocalDate date, LocalDate debut, LocalDate fin) {
        if (debut == null && fin == null) {
            return true;
        }
        if (date == null) {
            return false;
        }
        if (debut != null && date.isBefore(debut)) {
            return false;
        }
        return fin == null || !date.isAfter(fin);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Couleur de police du préfixe selon le type de ligne (designation colorée). */
    private static Font fontTypeObjet(String type) {
        return switch (type != null ? type : "") {
            case "PRESTATION" -> F_PREST;
            case "DEBOURS" -> F_DEB;
            case "ABONNEMENT_MENSUEL" -> F_ABO;
            default -> F_TD_GRAS;
        };
    }

    /** Préfixe lisible du type d'une ligne d'objet, utilisé dans la désignation. */
    private static String prefixObjet(String type) {
        return switch (type != null ? type : "") {
            case "PRESTATION" -> "Prestation";
            case "DEBOURS" -> "Débours";
            case "ABONNEMENT_MENSUEL" -> "Abonnement";
            default -> type != null ? type : "—";
        };
    }

    /** Paragraphe de désignation multi-lignes : préfixe coloré selon le type. */
    private static com.lowagie.text.Paragraph paragrapheObjets(FactureValideeFicheResponse f) {
        com.lowagie.text.Paragraph p = new Paragraph();
        p.setLeading(F_TD.getSize() * 1.35f);
        List<FactureValideeFicheResponse.ObjetLigne> objets = f.objets();
        if (objets == null || objets.isEmpty()) {
            p.add(new com.lowagie.text.Chunk(PdfFormats.valeur(f.objet()) != null
                    ? PdfFormats.valeur(f.objet()) : "—", F_TD));
            return p;
        }
        int cpt = 0;
        for (FactureValideeFicheResponse.ObjetLigne o : objets) {
            if (cpt++ > 0) {
                p.add(com.lowagie.text.Chunk.NEWLINE);
            }
            p.add(new com.lowagie.text.Chunk(prefixObjet(o.type()) + " : ", fontTypeObjet(o.type())));
            p.add(new com.lowagie.text.Chunk(o.libelle() != null ? o.libelle() : "—", F_TD));
        }
        return p;
    }

    /** Cellule DESIGNATION colorée par type de ligne. */
    private static PdfPCell cellDesig(FactureValideeFicheResponse f) {
        PdfPCell c = new PdfPCell(paragrapheObjets(f));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidthBottom(0.5f);
        c.setPaddingTop(4);
        c.setPaddingBottom(4);
        c.setPaddingLeft(14f);
        c.setPaddingRight(6);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private static String codeSociete(int id) {
        return "CLI-" + String.format("%04d", id);
    }

    private static String statutFacture(BigDecimal montant, BigDecimal paye) {
        BigDecimal m = nullToZero(montant);
        BigDecimal p = nullToZero(paye);
        BigDecimal reste = m.subtract(p).max(BigDecimal.ZERO);
        if (m.signum() <= 0) {
            return "—";
        }
        if (reste.signum() <= 0) {
            return "Soldée";
        }
        return p.signum() > 0 ? "Partielle" : "Impayée";
    }

    // ------------------------------------------------------------------
    // Contenu du document
    // ------------------------------------------------------------------

    private static void ecrire(Document doc, List<FactureValideeFicheResponse> fiches,
                               LocalDate debut, LocalDate fin, boolean asc) {
        if (fiches.isEmpty()) {
            doc.add(new Paragraph("Aucune facture ne correspond aux filtres appliqués.",
                    PdfTheme.F_PETIT_ITALIQUE));
            return;
        }
        doc.add(tableau(fiches));
        doc.add(signatures());
    }

    private static PdfPTable signatures() {
        PdfStyleFacture signaturesStyle = new PdfStyleFacture(PdfStyleFacture.ECHELLES[0]);
        PdfPTable signatures = PdfComposants.signaturesStyle(signaturesStyle, signaturesStyle.hSignature,
                new String[]{"Client"},
                new String[]{"Responsable Traitement", "Dossiers Clients"},
                new String[]{"Responsable Audit", "Contrôleur de Gestion"},
                new String[]{"Manager"});
        signatures.setSpacingBefore(40);
        return signatures;
    }

    private static PdfPTable tableau(List<FactureValideeFicheResponse> fiches) {
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.78f, 1.16f, 0.8f, 1.66f, 0.72f, 0.68f, 0.66f, 0.72f, 0.92f});
        table.setHeaderRows(1);
        table.setSpacingBefore(6);

        th(table, "DATE FACTURE", Element.ALIGN_LEFT);
        th(table, "RÉFÉRENCE FACTURE", Element.ALIGN_LEFT);
        th(table, "TYPE", Element.ALIGN_LEFT);
        th(table, "DÉSIGNATION", Element.ALIGN_LEFT);
        th(table, "MONTANT FACTURÉ", Element.ALIGN_RIGHT);
        th(table, "MONTANT ENCAISSÉ", Element.ALIGN_RIGHT);
        th(table, "MONTANT IMPAYÉ", Element.ALIGN_RIGHT);
        th(table, "ÉTAT DE PAIEMENT", Element.ALIGN_LEFT);
        th(table, "OBSERVATION", Element.ALIGN_LEFT);

        BigDecimal totalFacture = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;
        for (FactureValideeFicheResponse fiche : fiches) {
            ligneFacture(table, fiche);
            for (FactureValideeFicheResponse.PaiementFicheItem p : fiche.paiements()) {
                ligneReglement(table, p);
            }
            totalFacture = totalFacture.add(nullToZero(fiche.montant()));
            totalPaye = totalPaye.add(nullToZero(fiche.montantPaye()));
        }

        BigDecimal resteTotal = totalFacture.subtract(totalPaye).max(BigDecimal.ZERO);

        PdfPCell lbl = cellTd("Totaux (vue courante)", F_TD_GRAS, null, true);
        lbl.setColspan(4);
        lbl.setBorder(Rectangle.TOP);
        lbl.setBorderColor(PdfTheme.PRIMAIRE);
        lbl.setBorderWidthTop(1.2f);
        lbl.setPaddingTop(6);
        table.addCell(lbl);
        table.addCell(cellMontant(PdfFormats.formatMontant(totalFacture), F_TD_GRAS, null, true,
                true));
        table.addCell(cellMontant(PdfFormats.formatMontant(totalPaye), F_TD_GRAS, null, true,
                true));
        table.addCell(cellMontant(PdfFormats.formatMontant(resteTotal),
                resteTotal.signum() > 0 ? F_ROUGE : F_TD_GRAS, null, true, true));
        table.addCell(cellTd("", F_TD_GRAS, null, true));
        table.addCell(cellTd("", F_TD_GRAS, null, true));
        return table;
    }

    private static void ligneFacture(PdfPTable table, FactureValideeFicheResponse fiche) {
        BigDecimal montant = nullToZero(fiche.montant());
        BigDecimal paye = nullToZero(fiche.montantPaye());
        BigDecimal reste = nullToZero(fiche.resteAPayer());

        table.addCell(cellTd(date(fiche.dateFactureValidee()), F_TD, null, false, 8f));
        table.addCell(cellTd(PdfFormats.valeur(fiche.referenceFactureValidee()), F_TD_GRAS, null,
                false, 8f));
        table.addCell(cellTd("—", F_TD, null, false, 8f));
        table.addCell(cellDesig(fiche));
        table.addCell(cellMontant(PdfFormats.formatMontant(montant), F_TD, null, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(paye), F_TD, null, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(reste),
                reste.signum() > 0 ? F_ROUGE : F_TD, null, false, false));
        table.addCell(cellTd(statutFacture(montant, paye), F_TD, null, false, 8f));
        table.addCell(cellTd("—", F_TD, null, false, 8f));
    }

    private static void ligneReglement(PdfPTable table,
                                       FactureValideeFicheResponse.PaiementFicheItem p) {
        table.addCell(cellTd(date(p.datePaiement()), F_TD_ITALIQUE, null, false, 20f));
        table.addCell(cellTd(PdfFormats.valeur(p.referencePaiement()), F_TD_ITALIQUE, null, false, 20f));
        table.addCell(cellTd("Règlement", F_REGL, PdfTheme.VERT_PALE, false, 20f));
        table.addCell(cellTd(p.libelleMode() != null ? p.libelleMode() : "—", F_TD_ITALIQUE, null,
                false, 20f));
        table.addCell(cellMontant("—", F_TD_ITALIQUE, null, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(p.montantPaye()), F_TD_ITALIQUE, null,
                false, false));
        table.addCell(cellMontant("—", F_TD_ITALIQUE, null, false, false));
        table.addCell(cellTd("Encaissé", F_TD_ITALIQUE, null, false, 20f));
        table.addCell(cellTd(
                p.commentaire() != null && !p.commentaire().isBlank()
                        ? p.commentaire().trim() : "—", F_TD_ITALIQUE, null, false, 20f));
    }

    // ------------------------------------------------------------------
    // Helpers cellules / en-tête
    // ------------------------------------------------------------------

    private static String date(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "—";
    }

    private static void th(PdfPTable table, String texte, int alignement) {
        PdfPCell c = new PdfPCell(new Phrase(texte, F_TH));
        c.setBackgroundColor(PdfTheme.PRIMAIRE);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.VERT_MARQUE);
        c.setBorderWidthBottom(1.5f);
        c.setPaddingTop(6);
        c.setPaddingBottom(6);
        c.setPaddingLeft(6);
        c.setPaddingRight(6);
        c.setHorizontalAlignment(alignement);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(c);
    }

    /** Cellule de corps ; les retours à la ligne dans le texte sont honorés (désignation multi-lignes). */
    private static PdfPCell cellTd(String texte, Font police, Color fond,
                                   boolean total, float indentGauche) {
        Paragraph p = paragrapheMultiline(texte, police);
        PdfPCell c = new PdfPCell(p);
        if (fond != null) {
            c.setBackgroundColor(fond);
        }
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidthBottom(0.5f);
        c.setPaddingTop(4);
        c.setPaddingBottom(4);
        c.setPaddingLeft(6 + indentGauche);
        c.setPaddingRight(6);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (total) {
            c.setBorder(Rectangle.NO_BORDER);
        }
        return c;
    }

    private static PdfPCell cellTd(String texte, Font police, Color fond, boolean total) {
        return cellTd(texte, police, fond, total, 0f);
    }

    private static PdfPCell cellMontant(String texte, Font police, Color fond,
                                        boolean total, boolean top) {
        PdfPCell c = cellTd(texte, police, fond, total);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (top) {
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(PdfTheme.PRIMAIRE);
            c.setBorderWidthTop(1.2f);
            c.setPaddingTop(6);
        }
        return c;
    }

    /** Texte multi-lignes en Paragraph : préserve les sauts de ligne (désignation par service). */
    private static Paragraph paragrapheMultiline(String texte, Font police) {
        Paragraph p = new Paragraph();
        p.setLeading(police.getSize() * 1.35f);
        String[] lignes = texte != null ? texte.split("\n", -1) : new String[]{"—"};
        for (int i = 0; i < lignes.length; i++) {
            p.add(new com.lowagie.text.Chunk(lignes[i], police));
            if (i < lignes.length - 1) {
                p.add(com.lowagie.text.Chunk.NEWLINE);
            }
        }
        return p;
    }
}
