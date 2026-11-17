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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Facturation d'un client au format PDF (onglet « Facturation » de la fiche client).
 *
 * <p>Toutes les factures validées liées au client, groupées en facture générale
 * puis factures de paiement enfants, avec leurs règlements. Le filtre de dates
 * correspond à celui de l'onglet : début / fin sur la date de la facture générale,
 * et l'ordre des dates (croissant / décroissant).</p>
 */
final class PdfFacturationClient {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOIS_LABEL_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE);
    private static final Font F_ROUGE = PdfTheme.font(8, Font.BOLD, PdfTheme.ROUGE);
    private static final Font F_TH = PdfTheme.font(7, Font.BOLD, Color.WHITE);
    private static final Font F_TD = PdfTheme.font(8, Font.NORMAL, PdfTheme.TEXTE);
    private static final Font F_TD_GRAS = PdfTheme.font(8, Font.BOLD, PdfTheme.TEXTE);
    private static final Font F_TD_ITALIQUE = PdfTheme.font(7, Font.ITALIC, PdfTheme.GRIS_TEXTE);

    private PdfFacturationClient() {
    }

    static byte[] generer(Societe societe,
                          List<FactureValideeFicheResponse> fiches,
                          LocalDate debut, LocalDate fin, String ordre, String recherche) {
        boolean ordreAsc = "ASC".equalsIgnoreCase(ordre);
        List<Groupe> groupes = construireGroupes(fiches, debut, fin, ordreAsc, recherche);
        String client = societe != null && societe.getNomSociete() != null
                ? societe.getNomSociete() : "—";
        int idClient = societe != null && societe.getId() != null ? societe.getId() : 0;
        String enteteLigne = client + "  ·  " + codeSociete(idClient);
        return PdfDocument.generer(new com.lowagie.text.Rectangle(842, 595),
                PdfDocument.MiseEnPage.FACTURATION, enteteLigne,
                "Facturation client — " + client,
                doc -> ecrire(doc, societe, client, groupes, debut, fin, ordreAsc),
                "Erreur de génération du PDF de facturation client");
    }

    // ------------------------------------------------------------------
    // Regroupement (mêmes règles que le frontend)
    // ------------------------------------------------------------------

    private record Groupe(FactureValideeFicheResponse general,
                          List<FactureValideeFicheResponse> enfants) {

        LocalDate date() {
            if (general != null) {
                return general.dateFactureValidee();
            }
            return enfants.isEmpty() ? null : enfants.get(0).dateFactureValidee();
        }

        /** Montant facturé affiché : somme des enfants (ou propre montant pour une générale sans enfant). */
        BigDecimal montant(Groupe groupe) {
            if (groupe.enfants().isEmpty()) {
                return nullToZero(groupe.general() != null ? groupe.general().montant() : null);
            }
            return groupe.enfants().stream()
                    .map(e -> nullToZero(e.montant()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal encaisse(Groupe groupe) {
            BigDecimal direct = groupe.general() != null
                    ? sommePaiements(groupe.general().paiements()) : BigDecimal.ZERO;
            return groupe.enfants().stream()
                    .map(e -> nullToZero(e.montantPaye()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(direct);
        }
    }

    private record Totaux(BigDecimal montant, BigDecimal paye) {
        BigDecimal reste() {
            return montant.subtract(paye).max(BigDecimal.ZERO);
        }
    }

    /**
     * Facture générale réglée directement (sans sous-facture) : elle s'affiche
     * comme sa propre sous-facture pour connaître son objet et son type réel.
     */
    private static List<FactureValideeFicheResponse> enfantsEffectifs(Groupe groupe) {
        if (!groupe.enfants().isEmpty()) {
            return groupe.enfants();
        }
        if (groupe.general() == null) {
            return List.of();
        }
        FactureValideeFicheResponse g = groupe.general();
        String typeObjet = g.objetType() != null && !g.objetType().isBlank()
                ? g.objetType() : "PRESTATION";
        return List.of(new FactureValideeFicheResponse(
                g.id(), g.referenceFactureValidee(), typeObjet,
                g.factureGeneraleId(), g.referenceFactureGenerale(),
                g.demandeId(), g.referenceDemande(), g.objet(), g.objetType(),
                g.mois(), g.montant(), g.montantPaye(), g.resteAPayer(),
                g.dateFactureValidee(), g.paiements()));
    }

    private static List<Groupe> construireGroupes(List<FactureValideeFicheResponse> fiches,
                                                  LocalDate debut, LocalDate fin, boolean asc) {
        return construireGroupes(fiches, debut, fin, asc, null);
    }

    private static List<Groupe> construireGroupes(List<FactureValideeFicheResponse> fiches,
                                                  LocalDate debut, LocalDate fin, boolean asc,
                                                  String recherche) {
        Comparator<LocalDate> cmp = asc
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());

        Map<Integer, FactureValideeFicheResponse> parId = fiches.stream()
                .collect(Collectors.toMap(FactureValideeFicheResponse::id, f -> f,
                        (a, b) -> a, java.util.LinkedHashMap::new));

        List<FactureValideeFicheResponse> generales = parId.values().stream()
                .filter(f -> "GENERALE".equals(f.type()))
                .sorted(Comparator.comparing(FactureValideeFicheResponse::dateFactureValidee, cmp))
                .toList();
        List<FactureValideeFicheResponse> enfants = parId.values().stream()
                .filter(f -> !"GENERALE".equals(f.type()))
                .sorted(Comparator.comparing(FactureValideeFicheResponse::dateFactureValidee, cmp))
                .toList();

        Map<Integer, List<FactureValideeFicheResponse>> enfantsParGeneral = new java.util.LinkedHashMap<>();
        List<FactureValideeFicheResponse> orphelins = new ArrayList<>();
        for (FactureValideeFicheResponse e : enfants) {
            FactureValideeFicheResponse parent =
                    e.factureGeneraleId() != null ? parId.get(e.factureGeneraleId()) : null;
            if (parent != null && "GENERALE".equals(parent.type())) {
                enfantsParGeneral.computeIfAbsent(parent.id(), k -> new ArrayList<>()).add(e);
            } else {
                orphelins.add(e);
            }
        }

        // Filtre par recherche sur référence demande, facture et paiement.
        String terme = recherche != null ? recherche.trim().toLowerCase() : "";
        java.util.function.Predicate<FactureValideeFicheResponse> matchRecherche = f -> {
            if (terme.isEmpty()) {
                return true;
            }
            StringBuilder hay = new StringBuilder();
            hay.append(f.referenceFactureValidee() != null ? f.referenceFactureValidee() : "").append(' ');
            hay.append(f.referenceDemande() != null ? f.referenceDemande() : "").append(' ');
            f.paiements().forEach(p ->
                hay.append(p.referencePaiement() != null ? p.referencePaiement() : "").append(' '));
            return hay.toString().toLowerCase().contains(terme);
        };

        List<Groupe> groupes = new ArrayList<>();
        for (FactureValideeFicheResponse g : generales) {
            if (!inRange(g.dateFactureValidee(), debut, fin)) {
                continue;
            }
            List<FactureValideeFicheResponse> enfantsG =
                    enfantsParGeneral.getOrDefault(g.id(), List.of());
            if (!terme.isEmpty() && !matchRecherche.test(g)
                    && enfantsG.stream().noneMatch(matchRecherche)) {
                continue;
            }
            groupes.add(new Groupe(g, enfantsG));
        }
        for (FactureValideeFicheResponse e : orphelins) {
            if (!inRange(e.dateFactureValidee(), debut, fin)) {
                continue;
            }
            if (!terme.isEmpty() && !matchRecherche.test(e)) {
                continue;
            }
            groupes.add(new Groupe(null, List.of(e)));
        }
        groupes.sort(Comparator.comparing(Groupe::date, cmp));
        return groupes;
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

    private static BigDecimal sommePaiements(
            List<FactureValideeFicheResponse.PaiementFicheItem> paiements) {
        return paiements.stream()
                .map(FactureValideeFicheResponse.PaiementFicheItem::montantPaye)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String libelleType(String type) {
        return switch (type != null ? type : "") {
            case "GENERALE" -> "Générale";
            case "PRESTATION" -> "Prestation";
            case "DEBOURS" -> "Débours";
            case "ABONNEMENT_MENSUEL" -> "Abonnement";
            default -> type != null ? type : "—";
        };
    }

    private static String designationFacture(FactureValideeFicheResponse f) {
        if ("GENERALE".equals(f.type())) {
            return "Facture-" + PdfFormats.valeur(f.referenceDemande());
        }
        String objet = PdfFormats.valeur(f.objet());
        if ("ABONNEMENT_MENSUEL".equals(f.type()) && f.mois() != null) {
            // mois au format AAAA-MM
            int annee = Integer.parseInt(f.mois().substring(0, 4));
            int mois = Integer.parseInt(f.mois().substring(5, 7));
            String label = MOIS_LABEL_FMT.format(LocalDate.of(annee, mois, 1));
            objet = objet + " (" + Character.toUpperCase(label.charAt(0)) + label.substring(1) + ")";
        }
        return objet;
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

    private static void ecrire(Document doc, Societe societe, String client,
                               List<Groupe> groupes, LocalDate debut, LocalDate fin, boolean asc) {
        if (groupes.isEmpty()) {
            doc.add(new Paragraph("Aucune facture ne correspond aux filtres appliqués.",
                    PdfTheme.F_PETIT_ITALIQUE));
            return;
        }
        doc.add(tableau(groupes));
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

    private static PdfPTable tableau(List<Groupe> groupes) {
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.78f, 1.16f, 0.8f, 1.46f, 0.72f, 0.68f, 0.66f, 0.72f, 0.92f});
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

        for (Groupe groupe : groupes) {
            ligneGeneral(table, groupe);
            for (FactureValideeFicheResponse enfant : enfantsEffectifs(groupe)) {
                ligneEnfant(table, enfant);
                for (FactureValideeFicheResponse.PaiementFicheItem p : enfant.paiements()) {
                    ligneReglement(table, p);
                }
            }
            totalFacture = totalFacture.add(groupe.montant(groupe));
            totalPaye = totalPaye.add(groupe.encaisse(groupe));
        }

        Totaux totaux = new Totaux(totalFacture, totalPaye);

        PdfPCell lbl = cellTd("Totaux (vue courante)", F_TD_GRAS, null, true);
        lbl.setColspan(4);
        lbl.setBorder(Rectangle.TOP);
        lbl.setBorderColor(PdfTheme.PRIMAIRE);
        lbl.setBorderWidthTop(1.2f);
        lbl.setPaddingTop(6);
        table.addCell(lbl);
        table.addCell(cellMontant(PdfFormats.formatMontant(totaux.montant()), F_TD_GRAS, null, true,
                true));
        table.addCell(cellMontant(PdfFormats.formatMontant(totaux.paye()), F_TD_GRAS, null, true,
                true));
        table.addCell(cellMontant(PdfFormats.formatMontant(totaux.reste()),
                totaux.reste().signum() > 0 ? F_ROUGE : F_TD_GRAS, null, true, true));
        table.addCell(cellTd("", F_TD_GRAS, null, true));
        table.addCell(cellTd("", F_TD_GRAS, null, true));
        return table;
    }

    private static void ligneGeneral(PdfPTable table, Groupe groupe) {
        if (groupe.general() == null) {
            // Facture de paiement orpheline : ligne pleine-largeur, en gras maigre.
            FactureValideeFicheResponse seule = groupe.enfants().get(0);
            ligneEnfant(table, seule);
            return;
        }
        FactureValideeFicheResponse g = groupe.general();
        BigDecimal montant = groupe.montant(groupe);
        BigDecimal encaisse = groupe.encaisse(groupe);
        BigDecimal reste = montant.subtract(encaisse).max(BigDecimal.ZERO);

        table.addCell(cellTd(date(g.dateFactureValidee()), F_TD_GRAS, PdfTheme.VERT_PALE, false));
        table.addCell(cellTd(PdfFormats.valeur(g.referenceFactureValidee()), F_TD_GRAS,
                PdfTheme.VERT_PALE, false));
        table.addCell(cellTd(libelleType(g.type()), F_TD_GRAS, PdfTheme.VERT_PALE, false));
        table.addCell(cellTd(designationFacture(g), F_TD_GRAS, PdfTheme.VERT_PALE, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(montant), F_TD_GRAS,
                PdfTheme.VERT_PALE, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(encaisse), F_TD_GRAS,
                PdfTheme.VERT_PALE, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(reste),
                reste.signum() > 0 ? F_ROUGE : F_TD_GRAS, PdfTheme.VERT_PALE, false, false));
        table.addCell(cellTd(statutFacture(montant, encaisse), F_TD_GRAS,
                PdfTheme.VERT_PALE, false));
        table.addCell(cellTd("—", F_TD_GRAS, PdfTheme.VERT_PALE, false));
    }

    private static void ligneEnfant(PdfPTable table, FactureValideeFicheResponse enfant) {
        BigDecimal montant = nullToZero(enfant.montant());
        BigDecimal paye = nullToZero(enfant.montantPaye());
        BigDecimal reste = nullToZero(enfant.resteAPayer());

        table.addCell(cellTd(date(enfant.dateFactureValidee()), F_TD, null, false, 14f));
        table.addCell(cellTd(PdfFormats.valeur(enfant.referenceFactureValidee()), F_TD, null, false, 14f));
        table.addCell(cellTd(libelleType(enfant.type()), F_TD, null, false, 14f));
        table.addCell(cellTd(designationFacture(enfant), F_TD, null, false, 14f));
        table.addCell(cellMontant(PdfFormats.formatMontant(montant), F_TD, null, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(paye), F_TD, null, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(reste),
                reste.signum() > 0 ? F_ROUGE : F_TD, null, false, false));
        table.addCell(cellTd(statutFacture(montant, paye), F_TD, null, false));
        table.addCell(cellTd("—", F_TD, null, false));
    }

    private static void ligneReglement(PdfPTable table,
                                       FactureValideeFicheResponse.PaiementFicheItem p) {
        table.addCell(cellTd(date(p.datePaiement()), F_TD_ITALIQUE, null, false, 20f));
        table.addCell(cellTd(PdfFormats.valeur(p.referencePaiement()), F_TD_ITALIQUE, null, false, 20f));
        table.addCell(cellTd("Règlement", F_TD_ITALIQUE, null, false, 20f));
        table.addCell(cellTd(p.libelleMode() != null ? p.libelleMode() : "—", F_TD_ITALIQUE, null, false, 20f));
        table.addCell(cellMontant("—", F_TD_ITALIQUE, null, false, false));
        table.addCell(cellMontant(PdfFormats.formatMontant(p.montantPaye()), F_TD_ITALIQUE, null, false, false));
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

    private static PdfPCell cellTd(String texte, Font police, Color fond,
                                   boolean total, float indentGauche) {
        PdfPCell c = new PdfPCell(new Phrase(texte != null ? texte : "—", police));
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
        PdfPCell c = new PdfPCell(new Phrase(texte != null ? texte : "—", police));
        if (fond != null) {
            c.setBackgroundColor(fond);
        }
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidthBottom(0.5f);
        c.setPaddingTop(4);
        c.setPaddingBottom(4);
        c.setPaddingLeft(6);
        c.setPaddingRight(6);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (total) {
            c.setBorder(Rectangle.NO_BORDER);
        }
        return c;
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

}
