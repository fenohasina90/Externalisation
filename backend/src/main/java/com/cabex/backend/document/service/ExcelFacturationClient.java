package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.facture.dto.FactureValideeFicheResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
 * Export Excel de l'onglet « Facturation » de la fiche client.
 *
 * <p>Design moderne : logo CABEX, bande de titre CABEX (filets verts),
 * tableau en vert CABEX avec alternance par niveau hiérarchique,
 * bloc signatures en bas de page.</p>
 */
final class ExcelFacturationClient {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOIS_LABEL_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE);
    private static final String FORMAT_NOMBRE = "#,##0\" Ar\"";

    private static final XSSFColor COULEUR_VERT_FONCE = rgb(0x1F, 0x62, 0x0E);
    private static final XSSFColor COULEUR_VERT_MARQUE = rgb(0x57, 0xCB, 0x00);
    private static final XSSFColor COULEUR_VERT_PALE = rgb(0xE9, 0xF5, 0xDC);
    private static final XSSFColor COULEUR_GRIS_TEXTE = rgb(0x62, 0x6E, 0x5E);
    private static final XSSFColor COULEUR_GRIS_BORDURE = rgb(0xCF, 0xDC, 0xC6);
    private static final XSSFColor COULEUR_ROUGE = rgb(0xDC, 0x26, 0x26);
    private static final XSSFColor COULEUR_AMBRE = rgb(0xB4, 0x53, 0x09);
    private static final XSSFColor COULEUR_VERT_OK = rgb(0x1F, 0x6B, 0x3F);
    private static final XSSFColor COULEUR_BLANC = rgb(0xFF, 0xFF, 0xFF);
    private static final XSSFColor COULEUR_TEXTE_INDEX = rgb(0x14, 0x28, 0x1D);

    private ExcelFacturationClient() {
    }

    static byte[] generer(Societe societe,
                          List<FactureValideeFicheResponse> fiches,
                          LocalDate debut, LocalDate fin, String ordre, String recherche) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            boolean asc = "ASC".equalsIgnoreCase(ordre);
            List<Groupe> groupes = construireGroupes(fiches, debut, fin, asc, recherche);
            String client = societe != null && societe.getNomSociete() != null
                    ? societe.getNomSociete() : "—";
            int idClient = societe != null && societe.getId() != null ? societe.getId() : 0;

            XSSFSheet sheet = wb.createSheet("Facturation");
            sheet.setDisplayGridlines(false);
            sheet.setTabColor(COULEUR_VERT_FONCE);
            sheet.setHorizontallyCenter(true);
            sheet.setFitToPage(true);
            colonnes(sheet);
            feuilleImpression(sheet);

            int ligne = ecrireEntete(sheet, 0, client, idClient);
            int ligneHeader = ecrireHeader(sheet, ligne + 1);
            int courant = ligneHeader + 1;
            for (Groupe groupe : groupes) {
                courant = ecrireGroupe(sheet, courant, groupe);
            }
            courant = ecrireTotaux(sheet, courant, groupes);
            ecrireSignatures(sheet, courant + 2);
            ecrireNoteCabinet(sheet, courant + 8);

            ((XSSFSheet) sheet).setRepeatingRows(CellRangeAddress.valueOf("1:" + ligneHeader));
            sheet.createFreezePane(0, ligneHeader + 1);
            wb.setPrintArea(0, 0, 8, 0, courant + 9);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de génération de l'Excel de facturation client", e);
        }
    }

    private static XSSFColor rgb(int r, int g, int b) {
        return new XSSFColor(new byte[]{(byte) r, (byte) g, (byte) b}, null);
    }

    // ==================================================================
    // Groupes (mêmes règles que le frontend)
    // ==================================================================

    private record Groupe(FactureValideeFicheResponse general,
                          List<FactureValideeFicheResponse> enfants) {
        LocalDate date() {
            if (general != null) {
                return general.dateFactureValidee();
            }
            return enfants.isEmpty() ? null : enfants.get(0).dateFactureValidee();
        }

        BigDecimal montant() {
            if (enfants.isEmpty()) {
                return nullToZero(general != null ? general.montant() : null);
            }
            return enfants.stream()
                    .map(e -> nullToZero(e.montant()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal encaisse() {
            BigDecimal direct = general != null ? sommePaiements(general.paiements()) : BigDecimal.ZERO;
            return enfants.stream()
                    .map(e -> nullToZero(e.montantPaye()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(direct);
        }

        BigDecimal reste() {
            return montant().subtract(encaisse()).max(BigDecimal.ZERO);
        }
    }

    private static List<FactureValideeFicheResponse> enfantsEffectifs(Groupe groupe) {
        if (!groupe.enfants().isEmpty()) {
            return groupe.enfants();
        }
        if (groupe.general() == null) {
            return List.of();
        }
        FactureValideeFicheResponse g = groupe.general();
        String type = g.objetType() != null && !g.objetType().isBlank() ? g.objetType() : "PRESTATION";
        return List.of(new FactureValideeFicheResponse(
                g.id(), g.referenceFactureValidee(), type,
                g.factureGeneraleId(), g.referenceFactureGenerale(),
                g.demandeId(), g.referenceDemande(), g.objet(), g.objetType(),
                g.mois(), g.montant(), g.montantPaye(), g.resteAPayer(),
                g.dateFactureValidee(), g.paiements()));
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
            if (!inRange(g.dateFactureValidee(), debut, fin)) continue;
            List<FactureValideeFicheResponse> enfantsG = enfantsParGeneral.getOrDefault(g.id(), List.of());
            if (!terme.isEmpty() && !matchRecherche.test(g)
                    && enfantsG.stream().noneMatch(matchRecherche)) {
                continue;
            }
            groupes.add(new Groupe(g, enfantsG));
        }
        for (FactureValideeFicheResponse e : orphelins) {
            if (!inRange(e.dateFactureValidee(), debut, fin)) continue;
            if (!terme.isEmpty() && !matchRecherche.test(e)) continue;
            groupes.add(new Groupe(null, List.of(e)));
        }
        groupes.sort(Comparator.comparing(Groupe::date, cmp));
        return groupes;
    }

    private static LocalDate dateOrMin(LocalDate d) {
        return d != null ? d : LocalDate.of(1900, 1, 1);
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
            return "Facture-" + (f.referenceDemande() != null ? f.referenceDemande() : "—");
        }
        String objet = f.objet() != null && !f.objet().isBlank() ? f.objet() : "—";
        if ("ABONNEMENT_MENSUEL".equals(f.type()) && f.mois() != null) {
            int annee = Integer.parseInt(f.mois().substring(0, 4));
            int mois = Integer.parseInt(f.mois().substring(5, 7));
            String label = MOIS_LABEL_FMT.format(LocalDate.of(annee, mois, 1));
            objet = objet + " (" + Character.toUpperCase(label.charAt(0)) + label.substring(1) + ")";
        }
        return objet;
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

    // ==================================================================
    // Feuille : colonnes et mise en page
    // ==================================================================

    private static void colonnes(Sheet sheet) {
        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(1, 21 * 256);
        sheet.setColumnWidth(2, 17 * 256);
        sheet.setColumnWidth(3, 46 * 256);
        sheet.setColumnWidth(4, 19 * 256);
        sheet.setColumnWidth(5, 19 * 256);
        sheet.setColumnWidth(6, 19 * 256);
        sheet.setColumnWidth(7, 19 * 256);
        sheet.setColumnWidth(8, 34 * 256);
    }

    private static void feuilleImpression(XSSFSheet sheet) {
        sheet.setHorizontallyCenter(true);
        sheet.setFitToPage(true);
        org.apache.poi.xssf.usermodel.XSSFPrintSetup printSetup =
                (org.apache.poi.xssf.usermodel.XSSFPrintSetup) sheet.getPrintSetup();
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        printSetup.setOrientation(org.apache.poi.ss.usermodel.PrintOrientation.LANDSCAPE);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);
        printSetup.setLeftMargin(0.35);
        printSetup.setRightMargin(0.35);
        printSetup.setTopMargin(0.45);
        printSetup.setBottomMargin(0.45);
        printSetup.setHeaderMargin(0.22);
        printSetup.setFooterMargin(0.22);
        sheet.getHeader().setLeft("CABINET EXCELLENT");
        sheet.getHeader().setCenter("Facturation du client");
        sheet.getHeader().setRight("&D");
        sheet.getFooter().setLeft(
                "CABINET EXCELLENT · LOT II P 154 JC AVARADOHA, Antananarivo 101, Madagascar · accueil.cabex@gmail.com");
        sheet.getFooter().setCenter("");
        sheet.getFooter().setRight("Page &P sur &N");
    }

    // ==================================================================
    // En-tête CABEX : logo + bande de titre
    // ==================================================================

    private static int ecrireEntete(XSSFSheet sheet, int ligne, String client, int idClient) {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = sheet.getWorkbook();

        // Bande verte super-fine (filigrane décoratif) en haut
        Row strip = sheet.createRow(ligne);
        strip.setHeightInPoints(4f);
        for (int i = 0; i <= 8; i++) {
            Cell c = strip.createCell(i);
            c.setCellStyle(remplissage(wb, COULEUR_VERT_MARQUE));
        }
        sheet.createRow(ligne + 1).setHeightInPoints(6f);

        // Zone logo + titre sur 3 lignes
        Row logoRow = sheet.createRow(ligne + 2);
        logoRow.setHeightInPoints(115f);
        Row titreRow = sheet.createRow(ligne + 3);
        titreRow.setHeightInPoints(28f);
        Row codeRow = sheet.createRow(ligne + 4);
        codeRow.setHeightInPoints(20f);

        // Logo CABEX
        try (InputStream in = ExcelFacturationClient.class.getResourceAsStream(
                "/static/logo-cabinet-excellent.jpg")) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);
                org.apache.poi.ss.usermodel.CreationHelper helper = wb.getCreationHelper();
                org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
                org.apache.poi.ss.usermodel.ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(0);
                anchor.setRow1(ligne + 1);
                anchor.setCol2(2);
                anchor.setRow2(ligne + 5);
                anchor.setAnchorType(
                        org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE);
                drawing.createPicture(anchor, pictureIdx);
            }
        } catch (Exception ignored) {
            // Logo ignoré si le placement échoue — reste un fichier fonctionnel.
        }

        // Ligne « FACTURATION DU CLIENT »
        Row labelRow = sheet.createRow(ligne + 2);
        labelRow.setHeightInPoints(14f);
        Cell label = labelRow.createCell(3);
        label.setCellValue("F A C T U R A T I O N   D U   C L I E N T");
        label.setCellStyle(texteBoldCustom(wb, COULEUR_GRIS_TEXTE, (short) 9));

        // Nom du client (grand, vert foncé CABEX)
        Cell nom = titreRow.createCell(3);
        nom.setCellValue(client);
        nom.setCellStyle(texteBoldCustom(wb, COULEUR_VERT_FONCE, (short) 22));

        // Code CLI + date de génération
        Cell code = codeRow.createCell(3);
        code.setCellValue("CLI-" + String.format("%04d", idClient)
                + "   ·   Généré le " + DATE_FMT.format(LocalDate.now()));
        code.setCellStyle(texteItalicGris(wb, (short) 9));

        // Filet vert sous l'en-tête
        int sepRow = ligne + 5;
        Row sep = sheet.createRow(sepRow);
        sep.setHeightInPoints(3f);
        for (int i = 0; i <= 8; i++) {
            Cell c = sep.createCell(i);
            c.setCellStyle(remplissage(wb, COULEUR_VERT_FONCE));
        }
        return sepRow + 1;
    }

    private static int ecrireHeader(XSSFSheet sheet, int ligne) {
        Row header = sheet.createRow(ligne);
        header.setHeightInPoints(30f);
        String[] entetes = {"DATE FACTURE", "RÉFÉRENCE FACTURE", "TYPE", "DÉSIGNATION",
                "MONTANT FACTURÉ", "MONTANT ENCAISSÉ", "MONTANT IMPAYÉ",
                "ÉTAT DE PAIEMENT", "OBSERVATION"};
        for (int i = 0; i < entetes.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(entetes[i]);
            c.setCellStyle(styleEnteteTableau(sheet, i));
        }
        return ligne + 1;
    }

    // ==================================================================
    // Lignes du tableau
    // ==================================================================

    private static int ecrireGroupe(XSSFSheet sheet, int ligne, Groupe groupe) {
        FactureValideeFicheResponse g = groupe.general();
        if (g == null) {
            ligne = ecrireEnfant(sheet, ligne, groupe.enfants().get(0));
        } else {
            BigDecimal montant = groupe.montant();
            BigDecimal encaisse = groupe.encaisse();
            BigDecimal reste = groupe.reste();

            Row row = sheet.createRow(ligne);
            row.setHeightInPoints(24f);
            setCell(row, 0, DATE_FMT.format(dateOrMin(g.dateFactureValidee())), styleGeneral(sheet, 0));
            setCell(row, 1, g.referenceFactureValidee(), styleGeneral(sheet, 0));
            setCell(row, 2, libelleType(g.type()), styleGeneral(sheet, 0));
            setCell(row, 3, designationFacture(g), styleGeneral(sheet, 0));
            setChiffre(row, 4, montant, nombreGeneral(sheet));
            setChiffre(row, 5, encaisse, nombreGeneral(sheet));
            setChiffre(row, 6, reste, nombreGeneralRouge(sheet));
            setCell(row, 7, statutFacture(montant, encaisse), etatGeneral(sheet, montant, encaisse));
            setCell(row, 8, "—", styleGeneral(sheet, 0));
            ligne++;
        }
        for (FactureValideeFicheResponse enfant : enfantsEffectifs(groupe)) {
            ligne = ecrireEnfant(sheet, ligne, enfant);
            for (FactureValideeFicheResponse.PaiementFicheItem p : enfant.paiements()) {
                ligne = ecrireReglement(sheet, ligne, p);
            }
        }
        return ligne;
    }

    private static int ecrireEnfant(XSSFSheet sheet, int ligne, FactureValideeFicheResponse enfant) {
        BigDecimal montant = nullToZero(enfant.montant());
        BigDecimal paye = nullToZero(enfant.montantPaye());
        BigDecimal reste = nullToZero(enfant.resteAPayer());

        Row row = sheet.createRow(ligne);
        row.setHeightInPoints(20f);
        setCell(row, 0, DATE_FMT.format(dateOrMin(enfant.dateFactureValidee())),
                texteStandardIndente(sheet, 2));
        setCell(row, 1, enfant.referenceFactureValidee(), texteStandardIndente(sheet, 2));
        setCell(row, 2, libelleType(enfant.type()), texteStandardIndente(sheet, 2));
        setCell(row, 3, designationFacture(enfant), texteStandardIndente(sheet, 2));
        setChiffre(row, 4, montant, nombreStandardIndente(sheet));
        setChiffre(row, 5, paye, nombreStandardIndente(sheet));
        setChiffre(row, 6, reste, nombreImpayeeStandard(sheet, reste));
        setCell(row, 7, statutFacture(montant, paye), etatStandard(sheet, montant, paye));
        setCell(row, 8, "—", texteStandard(sheet));
        return ligne + 1;
    }

    private static int ecrireReglement(XSSFSheet sheet, int ligne,
                                       FactureValideeFicheResponse.PaiementFicheItem p) {
        Row row = sheet.createRow(ligne);
        row.setHeightInPoints(18f);
        setCell(row, 0, DATE_FMT.format(dateOrMin(p.datePaiement())), reglementIndente(sheet, 4));
        setCell(row, 1, p.referencePaiement(), reglementIndente(sheet, 4));
        setCell(row, 2, "Règlement", reglementIndente(sheet, 4));
        setCell(row, 3, p.libelleMode() != null ? p.libelleMode() : "—", reglementIndente(sheet, 4));
        setChiffre(row, 4, BigDecimal.ZERO, reglementNombre(sheet));
        setChiffre(row, 5,
                p.montantPaye() != null ? p.montantPaye() : BigDecimal.ZERO, reglementNombre(sheet));
        setChiffre(row, 6, BigDecimal.ZERO, reglementNombre(sheet));
        setCell(row, 7, "Encaissé", reglementEncaisse(sheet));
        setCell(row, 8,
                p.commentaire() != null && !p.commentaire().isBlank()
                        ? p.commentaire().trim() : "—",
                reglementIndente(sheet, 4));
        return ligne + 1;
    }

    // ==================================================================
    // Totaux + signatures + note CABEX
    // ==================================================================

    private static int ecrireTotaux(XSSFSheet sheet, int ligne, List<Groupe> groupes) {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = sheet.getWorkbook();
        BigDecimal totalFacture = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;
        for (Groupe groupe : groupes) {
            totalFacture = totalFacture.add(groupe.montant());
            totalPaye = totalPaye.add(groupe.encaisse());
        }
        BigDecimal reste = totalFacture.subtract(totalPaye).max(BigDecimal.ZERO);

        Row row = sheet.createRow(ligne);
        row.setHeightInPoints(24f);

        Cell label = row.createCell(0);
        sheet.addMergedRegion(new CellRangeAddress(ligne, ligne, 0, 3));
        label.setCellValue("TOTAUX (vue courante)");
        label.setCellStyle(totauxLabel(wb));

        setChiffreStyled(row, 4, totalFacture, totauxNombre(wb));
        setChiffreStyled(row, 5, totalPaye, totauxNombre(wb));
        setChiffreStyled(row, 6, reste, totauxImpayee(wb));

        Cell vide = row.createCell(7);
        vide.setCellStyle(totauxLabel(wb));
        Cell vide2 = row.createCell(8);
        vide2.setCellStyle(totauxLabel(wb));
        return ligne + 1;
    }

    private static void ecrireSignatures(XSSFSheet sheet, int ligne) {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = sheet.getWorkbook();
        int sigRow = ligne + 1;
        Row titre = sheet.createRow(sigRow);
        titre.setHeightInPoints(20f);
        Row zone = sheet.createRow(sigRow + 1);
        zone.setHeightInPoints(70f);

        String[] labels = {"Client",
                "Responsable Traitement — Dossiers Clients",
                "Responsable Audit — Contrôleur de Gestion",
                "Manager"};
        int[] cols = {0, 2, 4, 6};
        for (int i = 0; i < labels.length; i++) {
            int col = cols[i];
            sheet.addMergedRegion(new CellRangeAddress(sigRow, sigRow, col, col + 1));
            sheet.addMergedRegion(new CellRangeAddress(sigRow + 1, sigRow + 1, col, col + 1));

            Cell titreCell = titre.createCell(col);
            titreCell.setCellValue(labels[i]);
            titreCell.setCellStyle(signatureLabel(wb));

            Cell areaCell = zone.createCell(col);
            areaCell.setCellStyle(signatureArea(wb));
        }
    }

    private static void ecrireNoteCabinet(XSSFSheet sheet, int ligne) {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = sheet.getWorkbook();
        Row row = sheet.createRow(ligne);
        row.setHeightInPoints(14f);
        Cell c = row.createCell(0);
        sheet.addMergedRegion(new CellRangeAddress(ligne, ligne, 0, 8));
        c.setCellValue("CABINET EXCELLENT · LOT II P 154 JC AVARADOHA, Antananarivo 101, Madagascar · accueil.cabex@gmail.com");
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 8);
        f.setItalic(true);
        f.setColor(COULEUR_GRIS_TEXTE);
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFont(f);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        c.setCellStyle(cs);
    }

    // ==================================================================
    // Helpers cellule (définition + style)
    // ==================================================================

    private static void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private static void setChiffre(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        setChiffreStyled(row, col, value, style);
    }

    private static void setChiffreStyled(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.getCell(col);
        if (c == null) {
            c = row.createCell(col);
        }
        BigDecimal v = value != null ? value : BigDecimal.ZERO;
        c.setCellValue(v.doubleValue());
        c.setCellStyle(style);
    }

    private static XSSFCellStyle style(Workbook wb, XSSFCellStyle cs, XSSFFont f) {
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }

    private static org.apache.poi.xssf.usermodel.XSSFFont newFont(Workbook wb) {
        return ((org.apache.poi.xssf.usermodel.XSSFWorkbook) wb).createFont();
    }

    private static org.apache.poi.xssf.usermodel.XSSFFont font(Workbook wb, String name,
                                                              short size, boolean bold,
                                                              XSSFColor color) {
        XSSFFont f = newFont(wb);
        f.setFontName(name);
        f.setFontHeightInPoints(size);
        f.setBold(bold);
        if (color != null) {
            f.setColor(color);
        }
        return f;
    }

    private static XSSFCellStyle remplir(XSSFWorkbook wb, XSSFColor fillColor) {
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFillForegroundColor(fillColor);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cs;
    }

    private static XSSFCellStyle remplissage(Workbook wb, XSSFColor color) {
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFillForegroundColor(color);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cs;
    }

    private static XSSFCellStyle texteBoldCustom(Workbook wb, XSSFColor color,
                                                 short size) {
        XSSFFont f = font(wb, "Calibri", size, true, color);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }

    private static XSSFCellStyle texteItalicGris(Workbook wb, short size) {
        XSSFFont f = (XSSFFont) newFont(wb);
        f.setFontName("Calibri");
        f.setFontHeightInPoints(size);
        f.setItalic(true);
        f.setColor(COULEUR_GRIS_TEXTE);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }

    private static XSSFCellStyle styleEnteteTableau(Sheet sheet, int col) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 9, true, COULEUR_BLANC);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_FONCE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setWrapText(true);
        cs.setBorderBottom(BorderStyle.THIN);
        cs.setBottomBorderColor(COULEUR_VERT_MARQUE);
        cs.setAlignment(col >= 4 && col <= 6
                ? HorizontalAlignment.RIGHT : HorizontalAlignment.LEFT);
        return cs;
    }

    private static XSSFCellStyle styleGeneral(Sheet sheet, int indent) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, true, COULEUR_TEXTE_INDEX);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_PALE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) indent);
        return cs;
    }

    private static XSSFCellStyle nombreGeneral(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, true, COULEUR_TEXTE_INDEX);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_PALE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setAlignment(HorizontalAlignment.RIGHT);
        cs.setDataFormat(wb.createDataFormat().getFormat(FORMAT_NOMBRE));
        return cs;
    }

    private static XSSFCellStyle nombreGeneralRouge(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, true, COULEUR_ROUGE);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_PALE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setAlignment(HorizontalAlignment.RIGHT);
        cs.setDataFormat(wb.createDataFormat().getFormat(FORMAT_NOMBRE));
        return cs;
    }

    private static XSSFCellStyle texteStandard(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, false, null);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) 2);
        return cs;
    }

    private static XSSFCellStyle texteStandardIndente(Sheet sheet, int indent) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, false, null);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) indent);
        return cs;
    }

    private static XSSFCellStyle nombreStandardIndente(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, false, null);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) 2);
        cs.setAlignment(HorizontalAlignment.RIGHT);
        cs.setDataFormat(wb.createDataFormat().getFormat(FORMAT_NOMBRE));
        return cs;
    }


    private static XSSFCellStyle nombreImpayeeStandard(Sheet sheet, BigDecimal reste) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) newFont(wb);
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 10);
        f.setBold(reste.signum() > 0);
        f.setColor(reste.signum() > 0 ? COULEUR_ROUGE : COULEUR_VERT_OK);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) 2);
        cs.setAlignment(HorizontalAlignment.RIGHT);
        cs.setDataFormat(wb.createDataFormat().getFormat(FORMAT_NOMBRE));
        return cs;
    }

    private static XSSFCellStyle reglementIndente(Sheet sheet, int indent) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) newFont(wb);
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 9);
        f.setItalic(true);
        f.setColor(COULEUR_GRIS_TEXTE);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) indent);
        return cs;
    }

    private static XSSFCellStyle reglementNombre(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) newFont(wb);
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 9);
        f.setItalic(true);
        f.setColor(COULEUR_GRIS_TEXTE);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) 4);
        cs.setAlignment(HorizontalAlignment.RIGHT);
        cs.setDataFormat(wb.createDataFormat().getFormat(FORMAT_NOMBRE));
        return cs;
    }

    private static XSSFCellStyle reglementEncaisse(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();
        XSSFFont f = (XSSFFont) newFont(wb);
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 9);
        f.setBold(true);
        f.setItalic(true);
        f.setColor(COULEUR_VERT_OK);
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) 4);
        return cs;
    }

    private static XSSFCellStyle etatStandard(Sheet sheet, BigDecimal montant, BigDecimal paye) {
        Workbook wb = sheet.getWorkbook();
        String statut = statutFacture(montant, paye);
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, true, null);
        switch (statut) {
            case "Soldée", "Encaissé" -> f.setColor(COULEUR_VERT_OK);
            case "Partielle" -> f.setColor(COULEUR_AMBRE);
            case "Impayée" -> f.setColor(COULEUR_ROUGE);
            default -> f.setColor(COULEUR_GRIS_TEXTE);
        }
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setIndention((short) 2);
        return cs;
    }

    private static XSSFCellStyle etatGeneral(Sheet sheet, BigDecimal montant, BigDecimal paye) {
        Workbook wb = sheet.getWorkbook();
        String statut = statutFacture(montant, paye);
        XSSFFont f = (XSSFFont) font(wb, "Calibri", (short) 10, true, null);
        switch (statut) {
            case "Soldée", "Encaissé" -> f.setColor(COULEUR_VERT_OK);
            case "Partielle" -> f.setColor(COULEUR_AMBRE);
            case "Impayée" -> f.setColor(COULEUR_ROUGE);
            default -> f.setColor(COULEUR_GRIS_TEXTE);
        }
        XSSFCellStyle cs = (XSSFCellStyle) wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_PALE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }

    private static XSSFCellStyle totauxLabel(org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 10);
        f.setBold(true);
        f.setColor(COULEUR_BLANC);
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_FONCE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setBorderTop(BorderStyle.MEDIUM);
        cs.setTopBorderColor(COULEUR_TEXTE_INDEX);
        return cs;
    }

    private static XSSFCellStyle totauxNombre(org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 10);
        f.setBold(true);
        f.setColor(COULEUR_BLANC);
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_FONCE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setAlignment(HorizontalAlignment.RIGHT);
        cs.setDataFormat(wb.createDataFormat().getFormat(FORMAT_NOMBRE));
        cs.setBorderTop(BorderStyle.MEDIUM);
        cs.setTopBorderColor(COULEUR_TEXTE_INDEX);
        return cs;
    }

    private static XSSFCellStyle totauxImpayee(org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 10);
        f.setBold(true);
        f.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xB4, (byte) 0xB4}, null));
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFont(f);
        cs.setFillForegroundColor(COULEUR_VERT_FONCE);
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setAlignment(HorizontalAlignment.RIGHT);
        cs.setDataFormat(wb.createDataFormat().getFormat(FORMAT_NOMBRE));
        cs.setBorderTop(BorderStyle.MEDIUM);
        cs.setTopBorderColor(COULEUR_TEXTE_INDEX);
        return cs;
    }

    private static XSSFCellStyle signatureArea(org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setBorderBottom(BorderStyle.THIN);
        cs.setBottomBorderColor(new XSSFColor(new byte[]{0x6B, 0x72, (byte) 0x80}, null));
        return cs;
    }

    private static XSSFCellStyle signatureLabel(org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 9);
        f.setItalic(true);
        f.setColor(COULEUR_GRIS_TEXTE);
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFont(f);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }
}
