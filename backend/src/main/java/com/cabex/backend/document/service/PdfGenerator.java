package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.paiement.entity.Paiement;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Génération des documents PDF du cabinet (récapitulatif de demande, reçu de
 * paiement, plan de paiement, facture proforma).
 *
 * Charte graphique commune : vert foncé + vert vif et rouge du logo, en-tête avec logo et
 * pied de page paginé ("Page X / Y") sur les documents d'information,
 * tableaux à lignes alternées, montants alignés à droite.
 */
@Component
public class PdfGenerator {

    // ------------------------------------------------------------------
    // Identité du cabinet (à modifier ici uniquement)
    // ------------------------------------------------------------------
    private static final String NOM_CABINET = "CABINET EXCELLENT";
    private static final String ACTIVITE = "Études et externalisation de services";
    private static final String ADRESSE_SIEGE = "LOT II P 154 JC AVARADOHA, Antananarivo 101, Madagascar";
    private static final String NIF = "6001199978";
    private static final String STAT = "71102 11 2013 0 02016";
    private static final String RCS = "2014A01518";
    private static final String MAIL_ACCUEIL = "accueil.cabex@gmail.com";
    private static final String FACEBOOK = "Cabinet Excellent";
    private static final String CONTACT_TANA = "034 44 400 22";
    private static final String CONTACT_MAJUNGA = "034 32 811 66";
    private static final String MAIL_NAKAY = "cabinet.nakay@gmail.com";

    private static final String[] COORDONNEES_BANCAIRES = {
            "BAOBAB BANQUE MADAGASCAR Ambodivona",
            "00013 - 00011 - 24798687313 - 64",
            "Au nom de : CABINET EXCELLENT",
            ADRESSE_SIEGE
    };

    private static final String[] GROUPE = {
            "Agence Immo Excellent",
            "Agence de recrutement Excellent",
            "Agence Fiduciaire Excellent",
            "Institut Supérieur Excellent",
            "Tantsaha Production",
            "Cabinet Nakay Mahajanga",
            "Restaurant Nakay Mahajanga",
            "Association Nakay"
    };

    // ------------------------------------------------------------------
    // Palette (issue du logo : vert vif #57CB00, vert pâle #D4FFB0, rouge #FF0000)
    // ------------------------------------------------------------------
    /** Vert foncé : en-têtes de tableaux, titres, bandeaux (texte blanc lisible). */
    private static final Color PRIMAIRE = new Color(31, 98, 14);
    /** Vert vif du logo : filets et pastilles. */
    private static final Color VERT_MARQUE = new Color(87, 203, 0);
    /** Vert du texte de marque (CAB / EX), un peu assombri pour la lisibilité sur blanc. */
    private static final Color VERT_TEXTE = new Color(64, 168, 0);
    /** Rouge du logo (INET / CELLENT) : touche d'accent uniquement. */
    private static final Color ROUGE = new Color(224, 32, 32);
    /** Vert très pâle (cartes, bandes de section). */
    private static final Color VERT_PALE = new Color(238, 250, 226);
    /** Alternance de lignes des tableaux. */
    private static final Color ZEBRA = new Color(247, 252, 243);
    private static final Color GRIS_BORDURE = new Color(207, 220, 198);
    private static final Color GRIS_TEXTE = new Color(98, 110, 94);
    private static final Color TEXTE = new Color(32, 38, 32);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font F_TITRE = font(20, Font.BOLD, PRIMAIRE);
    private static final Font F_SOUS_TITRE = font(10, Font.NORMAL, GRIS_TEXTE);
    private static final Font F_SECTION = font(10, Font.BOLD, PRIMAIRE);
    private static final Font F_CARTE_TITRE = font(8, Font.BOLD, PRIMAIRE);
    private static final Font F_LABEL = font(7, Font.BOLD, GRIS_TEXTE);
    private static final Font F_LABEL_BLANC = font(8, Font.BOLD, new Color(212, 255, 176));
    private static final Font F_VALEUR = font(9.5f, Font.NORMAL, TEXTE);
    private static final Font F_VALEUR_GRAS = font(9.5f, Font.BOLD, TEXTE);
    private static final Font F_KPI = font(13, Font.BOLD, PRIMAIRE);
    private static final Font F_KPI_BLANC = font(13, Font.BOLD, Color.WHITE);
    private static final Font F_MONTANT_XL = font(24, Font.BOLD, Color.WHITE);
    private static final Font F_TH = font(8, Font.BOLD, Color.WHITE);
    private static final Font F_TD = font(9, Font.NORMAL, TEXTE);
    private static final Font F_TD_GRAS = font(9, Font.BOLD, TEXTE);
    private static final Font F_TD_PETIT = font(7.5f, Font.NORMAL, GRIS_TEXTE);
    private static final Font F_TD_GRAS_BLANC = font(10, Font.BOLD, Color.WHITE);
    private static final Font F_PETIT = font(8, Font.NORMAL, TEXTE);
    private static final Font F_PETIT_GRAS = font(8, Font.BOLD, TEXTE);
    private static final Font F_PETIT_ITALIQUE = font(8, Font.ITALIC, GRIS_TEXTE);
    private static final Font F_NOTE = font(9, Font.BOLDITALIC, PRIMAIRE);
    private static final Font F_MENTION = font(7, Font.NORMAL, GRIS_TEXTE);
    private static final Font F_MENTION_GRAS = font(7, Font.BOLD, TEXTE);
    private static final Font F_CASE = new Font(Font.ZAPFDINGBATS, 9, Font.NORMAL, PRIMAIRE);

    private final ConfigurationService configurationService;

    public PdfGenerator(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    private static Font font(float taille, int style, Color couleur) {
        return new Font(Font.HELVETICA, taille, style, couleur);
    }

    // ==================================================================
    //  DOCUMENTS
    // ==================================================================

    public byte[] recapitulatifDemande(DemandeService d) {
        return generer("Récapitulatif de demande", true,
                "Récapitulatif de demande " + valeur(d.getReferenceDemande()),
                doc -> {
                    Societe s = d.getSociete();

                    // Badge EXTERNALISATION
                    PdfPTable badge = new PdfPTable(1);
                    badge.setWidthPercentage(62);
                    badge.setHorizontalAlignment(Element.ALIGN_CENTER);
                    PdfPCell badgeCell = new PdfPCell(new Phrase("EXTERNALISATION",
                            font(17, Font.BOLD, Color.WHITE)));
                    badgeCell.setBackgroundColor(new Color(31, 41, 55));
                    badgeCell.setBorder(Rectangle.NO_BORDER);
                    badgeCell.setPadding(7);
                    badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    badge.addCell(badgeCell);
                    doc.add(badge);

                    Paragraph commercial = new Paragraph(
                            "Nom du commercial : " + valeur(d.getCommercial() != null
                                    ? d.getCommercial().getNomCommercial() : null),
                            F_VALEUR);
                    commercial.setAlignment(Element.ALIGN_RIGHT);
                    commercial.setSpacingBefore(6);
                    commercial.setSpacingAfter(4);
                    doc.add(commercial);

                    String im = d.getCommercial() != null
                            ? d.getCommercial().getImmatricule() : null;
                    ficheStdChamp(doc, "NOM DE L'ENTREPRISE OU DE LA SOCIETE",
                            valeur(s != null ? s.getNomSociete() : null)
                                    + "      N° IM : " + valeur(im));
                    ficheStdChamp(doc, "NOM DU CLIENT/RESPONSABLE",
                            valeur(d.getClientResponsable() != null
                                    ? d.getClientResponsable().getNomClient() : null));
                    ficheStdChamp(doc, "ACTIVITES",
                            valeur(s != null ? s.getActivite() : null));
                    ficheStdChamp(doc, "CONTACT",
                            valeur(s != null ? s.getContact() : null));
                    ficheStdChamp(doc, "MAIL et Fb",
                            valeur(s != null ? s.getEmail() : null)
                                    + (s != null && s.getFb() != null && !s.getFb().isBlank()
                                    ? " / " + s.getFb() : ""));
                    ficheStdChamp(doc, "ADRESSE",
                            valeur(s != null ? s.getAdresse() : null));
                    ficheStdChamp(doc, "DUREE D'EXISTENCE",
                            dureeExistence(s)
                                    + "      Nombre du PERSONNEL : "
                                    + (s != null && s.getNombrePersonnel() != null
                                    ? s.getNombrePersonnel().toString() : ""));
                    ficheStdChamp(doc, "CHIFFRE D'AFFAIRE MENSUEL",
                            s != null && s.getChiffreAffaireMensuel() != null
                                    ? formatArgent(s.getChiffreAffaireMensuel()) : "");

                    section(doc, "DOMAINE");
                    String domaine = s != null && s.getDomaine() != null
                            ? s.getDomaine().getNomDomaine() : "";
                    doc.add(ficheStdCases(domaine, "Comptabilité", "Fiscale", "Audit interne"));

                    section(doc, "SERVICES SOUHAITES");
                    if (d.getLignes() != null && !d.getLignes().isEmpty()) {
                        for (LigneService l : d.getLignes()) {
                            Paragraph ligne = new Paragraph();
                            ligne.setSpacingBefore(2);
                            ligne.add(new Chunk("[X] ", F_VALEUR_GRAS));
                            ligne.add(new Chunk(valeur(l.getService() != null
                                    ? l.getService().getNomService() : null)
                                    + "  (" + valeurDate(l.getDebutService())
                                    + " → " + valeurDate(l.getFinService()) + ")",
                                    F_VALEUR));
                            doc.add(ligne);
                        }
                    } else {
                        Paragraph aucun = new Paragraph("—", F_VALEUR);
                        aucun.setSpacingBefore(2);
                        doc.add(aucun);
                    }

                    Paragraph delai = new Paragraph("Délai d'exécution souhaité par le client : ",
                            F_VALEUR_GRAS);
                    delai.setSpacingBefore(10);
                    doc.add(delai);

                    Paragraph observation = new Paragraph("OBSERVATION :", F_NOTE);
                    observation.setSpacingBefore(8);
                    doc.add(observation);
                    String commentaire = d.getCommentaire();
                    Paragraph obsTexte = new Paragraph(
                            commentaire == null || commentaire.isBlank() ? "" : commentaire,
                            F_VALEUR);
                    obsTexte.setSpacingBefore(2);
                    obsTexte.setSpacingAfter(2);
                    doc.add(obsTexte);

                    PdfPTable signatures = new PdfPTable(4);
                    signatures.setWidthPercentage(100);
                    signatures.setSpacingBefore(10);
                    signatures.setKeepTogether(true);
                    for (String qui : new String[]{"Le Client", "Le Commercial",
                            "Le Responsable du dossier", "Manager"}) {
                        PdfPCell nom = new PdfPCell(new Phrase(qui, F_VALEUR_GRAS));
                        nom.setBorder(Rectangle.NO_BORDER);
                        nom.setPaddingBottom(2);
                        signatures.addCell(nom);
                    }
                    for (int k = 0; k < 4; k++) {
                        PdfPCell vide = new PdfPCell(new Phrase(" ", F_VALEUR));
                        vide.setBorder(Rectangle.NO_BORDER);
                        vide.setBorderWidthBottom(0.5f);
                        vide.setBorderColorBottom(GRIS_BORDURE);
                        vide.setFixedHeight(52);
                        signatures.addCell(vide);
                    }
                    doc.add(signatures);
                },
                "Erreur de génération du récapitulatif de demande");
    }

    public byte[] recuPaiement(Paiement p) {
        return generer("Reçu de paiement", true,
                "Reçu de paiement " + valeur(p.getReferencePaiement()),
                doc -> {
                    String client = p.getDemande() != null && p.getDemande().getSociete() != null
                            ? p.getDemande().getSociete().getNomSociete() : null;

                    titreDocument(doc, "Reçu de paiement",
                            "Référence paiement : " + valeur(p.getReferencePaiement()));

                    // Bandeau du montant reçu
                    PdfPCell montant = new PdfPCell();
                    montant.setBackgroundColor(PRIMAIRE);
                    montant.setBorder(Rectangle.NO_BORDER);
                    montant.setPadding(16);
                    Paragraph lib = new Paragraph("MONTANT REÇU", F_LABEL_BLANC);
                    lib.setAlignment(Element.ALIGN_CENTER);
                    montant.addElement(lib);
                    Paragraph val = new Paragraph(formatArgent(p.getMontantPaye()), F_MONTANT_XL);
                    val.setAlignment(Element.ALIGN_CENTER);
                    val.setSpacingBefore(4);
                    montant.addElement(val);
                    PdfPTable bandeau = new PdfPTable(1);
                    bandeau.setWidthPercentage(100);
                    bandeau.setSpacingAfter(14);
                    bandeau.addCell(montant);
                    doc.add(bandeau);

                    PdfPCell cClient = carte("Client et demande", new String[][]{
                            {"Client", valeur(client)},
                            {"Demande", valeur(p.getDemande() != null
                                    ? p.getDemande().getReferenceDemande() : null)}
                    });
                    PdfPCell cReglement = carte("Règlement", new String[][]{
                            {"Date de paiement", valeurDate(p.getDatePaiement())},
                            {"Mode de paiement", valeur(p.getModePaiement() != null
                                    ? p.getModePaiement().getLibelle() : null)},
                            {"Référence virement", valeur(p.getReferenceVirement())}
                    });
                    rangee(doc, 0.5f, cClient, cReglement);

                    if (p.getMontantPaye() != null) {
                        doc.add(encartLettres("Arrêté le présent reçu à la somme de ",
                                MontantEnLettres.convertir(p.getMontantPaye().longValue())
                                        .toUpperCase(Locale.FRANCE) + " ARIARY"));
                    }

                    String enregistrePar = p.getCreatedBy() != null
                            ? nomComplet(p.getCreatedBy().getPrenom(), p.getCreatedBy().getNom()) : null;
                    PdfPTable signatures = new PdfPTable(2);
                    signatures.setWidthPercentage(100);
                    signatures.setSpacingBefore(26);
                    signatures.setKeepTogether(true);
                    signatures.setWidths(new float[]{1f, 1f});
                    signatures.addCell(celluleSignature("Le client", null));
                    signatures.addCell(celluleSignature("Pour le cabinet",
                            enregistrePar != null ? "Enregistré par " + enregistrePar : null));
                    signatures.addCell(celluleEspaceSignature(70));
                    signatures.addCell(celluleEspaceSignature(70));
                    doc.add(signatures);
                },
                "Erreur de génération du reçu de paiement");
    }

    public byte[] planPaiement(ProposPaiement propos) {
        return generer("Plan de paiement", true,
                "Plan de paiement",
                doc -> {
                    String refDemande = propos.getDemande() != null
                            ? propos.getDemande().getReferenceDemande() : null;

                    titreDocument(doc, "Plan de paiement", "Demande : " + valeur(refDemande));

                    // Total payé = somme des échéances (cohérent avec le tableau)
                    BigDecimal totalAttendu = BigDecimal.ZERO;
                    BigDecimal totalPaye = BigDecimal.ZERO;
                    if (propos.getEcheances() != null) {
                        for (EcheancePaiement e : propos.getEcheances()) {
                            totalAttendu = totalAttendu.add(montantOuZero(e.getMontantAttendu()));
                            totalPaye = totalPaye.add(montantOuZero(e.getMontantPaye()));
                        }
                    }

                    rangee(doc, 0.4f,
                            kpi("MONTANT TOTAL", formatArgent(propos.getMontantTotal()), false),
                            kpi("DÉJÀ PAYÉ", formatArgent(totalPaye), false),
                            kpi("RESTE À PAYER", formatArgent(propos.getRestePayer()), true));

                    PdfPCell cDemande = carte("Demande", new String[][]{
                            {"Référence", valeur(refDemande)},
                            {"Nombre d'échéances", propos.getNombreEcheances() != null
                                    ? propos.getNombreEcheances().toString() : "-"}
                    });
                    PdfPCell cPeriode = carte("Période", new String[][]{
                            {"Début", valeurDate(propos.getDateDebut())},
                            {"Fin", valeurDate(propos.getDateFin())}
                    });
                    rangee(doc, 0.5f, cDemande, cPeriode);

                    section(doc, "Échéancier");
                    PdfPTable table = new PdfPTable(5);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{1.1f, 2.4f, 3f, 3f, 3f});
                    table.setHeaderRows(1);
                    entete(table, "N°", Element.ALIGN_CENTER);
                    entete(table, "DATE D'ÉCHÉANCE", Element.ALIGN_LEFT);
                    entete(table, "MONTANT ATTENDU", Element.ALIGN_RIGHT);
                    entete(table, "MONTANT PAYÉ", Element.ALIGN_RIGHT);
                    entete(table, "RESTE", Element.ALIGN_RIGHT);

                    int i = 0;
                    if (propos.getEcheances() != null && !propos.getEcheances().isEmpty()) {
                        for (EcheancePaiement e : propos.getEcheances()) {
                            boolean zebra = i++ % 2 == 1;
                            BigDecimal reste = montantOuZero(e.getMontantAttendu())
                                    .subtract(montantOuZero(e.getMontantPaye())).max(BigDecimal.ZERO);
                            corps(table, e.getNumeroEcheance() != null
                                    ? e.getNumeroEcheance().toString() : "-", Element.ALIGN_CENTER, zebra);
                            corps(table, valeurDate(e.getDateEcheance()), Element.ALIGN_LEFT, zebra);
                            corps(table, formatArgent(e.getMontantAttendu()), Element.ALIGN_RIGHT, zebra);
                            corps(table, formatArgent(e.getMontantPaye()), Element.ALIGN_RIGHT, zebra);
                            corps(table, formatArgent(reste), Element.ALIGN_RIGHT, zebra);
                        }
                        celluleTotal(table, "TOTAL", Element.ALIGN_RIGHT, 2);
                        celluleTotal(table, formatArgent(totalAttendu), Element.ALIGN_RIGHT, 1);
                        celluleTotal(table, formatArgent(totalPaye), Element.ALIGN_RIGHT, 1);
                        celluleTotal(table, formatArgent(totalAttendu.subtract(totalPaye).max(BigDecimal.ZERO)),
                                Element.ALIGN_RIGHT, 1);
                    } else {
                        PdfPCell vide = base(new Phrase("Aucune échéance enregistrée", F_PETIT_ITALIQUE),
                                Element.ALIGN_CENTER);
                        vide.setColspan(5);
                        vide.setBorder(Rectangle.BOTTOM);
                        vide.setBorderColor(GRIS_BORDURE);
                        table.addCell(vide);
                    }
                    doc.add(table);
                },
                "Erreur de génération du plan de paiement");
    }

    // ==================================================================
    //  FACTURE PROFORMA : toujours sur UNE SEULE page
    // ==================================================================

    /** Hauteur réservée en bas de page pour le pied (groupe, contacts, mentions légales). */
    private static final float HAUTEUR_PIED_FACTURE = 104f;
    private static final float MARGE_FACTURE = 32f;

    /**
     * Échelles de compacité essayées tour à tour : la facture est d'abord générée en
     * mise en page « confort » ; si elle déborde sur une 2e page, elle est regénérée plus
     * compacte (espacements, signatures, puis polices) jusqu'à tenir sur une page.
     */
    private static final float[] ECHELLES_FACTURE = {1f, 0.92f, 0.84f, 0.76f, 0.68f, 0.60f, 0.52f};

    /**
     * Facture proforma au format du modèle CABEX : en-tête (logo, identité, titre),
     * bandeau numéro / date / demande, destinataire, tableau des prestations, totaux
     * HT/TVA/TTC avec montant en lettres et observations, conditions de règlement,
     * signatures. Le groupe, les contacts et les mentions légales sont ancrés en bas
     * de page. Le résultat tient toujours sur une seule page (tant que le nombre de
     * lignes reste raisonnable : une quinzaine de lignes restent lisibles).
     */
    public byte[] factureProforma(Facture f) {
        byte[] pdf = null;
        for (float echelle : ECHELLES_FACTURE) {
            pdf = construireFacture(f, new StyleFacture(echelle));
            if (nombrePages(pdf) <= 1) {
                break;
            }
        }
        return pdf;
    }

    private static int nombrePages(byte[] pdf) {
        try {
            PdfReader lecteur = new PdfReader(pdf);
            int n = lecteur.getNumberOfPages();
            lecteur.close();
            return n;
        } catch (Exception e) {
            return 1;
        }
    }

    /** Dimensions et polices de la facture, dérivées d'une échelle de compacité (1 = confort). */
    private static final class StyleFacture {
        final Font th, td, tdGras, tdPetit, tdBlanc, label, valeurGras, petit, petitGras, mention,
                carteTitre, section, note, titre, sousTitre, caseCocher;
        final float tailleMeta, padV, padH, padCarte, ecart, hSignature, hObservations, logo;

        StyleFacture(float s) {
            // Les polices rétrécissent deux fois moins vite que les espacements (lisibilité).
            float f = 1f - (1f - s) / 2f;
            th = font(8f * f, Font.BOLD, Color.WHITE);
            td = font(9f * f, Font.NORMAL, TEXTE);
            tdGras = font(9f * f, Font.BOLD, TEXTE);
            tdPetit = font(7.5f * f, Font.NORMAL, GRIS_TEXTE);
            tdBlanc = font(10f * f, Font.BOLD, Color.WHITE);
            label = font(7.5f * f, Font.NORMAL, GRIS_TEXTE);
            valeurGras = font(9f * f, Font.BOLD, TEXTE);
            petit = font(8f * f, Font.NORMAL, TEXTE);
            petitGras = font(8f * f, Font.BOLD, TEXTE);
            mention = font(7f * f, Font.NORMAL, GRIS_TEXTE);
            carteTitre = font(8f * f, Font.BOLD, PRIMAIRE);
            section = font(9f * f, Font.BOLD, PRIMAIRE);
            note = font(9f * f, Font.BOLDITALIC, PRIMAIRE);
            titre = font(26f * f, Font.BOLD, PRIMAIRE);
            sousTitre = font(10f * f, Font.BOLD, ROUGE);
            caseCocher = new Font(Font.ZAPFDINGBATS, 9f * f, Font.NORMAL, PRIMAIRE);
            tailleMeta = 11f * f;
            padV = 5.5f * s;
            padH = 7f;
            padCarte = 9f * s + 1f;
            ecart = 11f * s + 3f;
            hSignature = 52f * s;
            hObservations = 46f * s;
            logo = 62f * f;
        }
    }

    private byte[] construireFacture(Facture f, StyleFacture st) {
        return genererFacture("Facture proforma " + valeur(f.getReferenceFacture()),
                doc -> {
                    DemandeService demande = f.getDemande();
                    Societe societe = demande != null ? demande.getSociete() : null;

                    faEntete(doc, f, st);

                    // ---- Destinataire & références ----
                    PdfPCell cDest = faCarte(st, "Facturé à", new String[][]{
                            {"Raison sociale", valeur(societe != null ? societe.getNomSociete() : null)},
                            {"Adresse", valeur(societe != null ? societe.getAdresse() : null)},
                            {"Contact", valeur(societe != null ? societe.getContact() : null)},
                            {"Mail", valeur(societe != null ? societe.getEmail() : null)},
                            {"NIF / STAT / RCS", "..................  /  ..................  /  .................."}
                    });
                    PdfPCell cRef = faCarte(st, "Informations", new String[][]{
                            {"Responsable", valeur(demande != null && demande.getClientResponsable() != null
                                    ? demande.getClientResponsable().getNomClient() : null)},
                            {"Demande", valeur(demande != null ? demande.getReferenceDemande() : null)},
                            {"Règlement", "À payer dès la réception"}
                    });
                    rangee(doc, 0.3f, cDest, cRef);

                    // ---- Tableau des prestations ----
                    PdfPTable table = new PdfPTable(6);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{4.4f, 1.5f, 0.7f, 1.9f, 1.3f, 1.9f});
                    table.setHeaderRows(1);
                    table.setSpacingBefore(st.ecart);
                    faTh(table, st, "DÉSIGNATION", Element.ALIGN_LEFT);
                    faTh(table, st, "PU\n(Ariary)", Element.ALIGN_RIGHT);
                    faTh(table, st, "QTÉ", Element.ALIGN_CENTER);
                    faTh(table, st, "MONTANT HTVA\n(Ariary)", Element.ALIGN_RIGHT);
                    faTh(table, st, "REMISE\n(Ariary)", Element.ALIGN_RIGHT);
                    faTh(table, st, "NET COMMERCIAL\n(Ariary)", Element.ALIGN_RIGHT);

                    PdfPCell bande = faCellule(st, new Phrase("PRESTATIONS", st.section), Element.ALIGN_LEFT);
                    bande.setColspan(6);
                    bande.setBackgroundColor(VERT_PALE);
                    bande.setBorder(Rectangle.LEFT);
                    bande.setBorderWidthLeft(2.5f);
                    bande.setBorderColorLeft(VERT_MARQUE);
                    table.addCell(bande);

                    BigDecimal totalHt = BigDecimal.ZERO;
                    int i = 0;
                    if (f.getLignes() != null) {
                        for (LigneService l : f.getLignes()) {
                            boolean zebra = i++ % 2 == 1;
                            Phrase designation = new Phrase();
                            designation.setLeading(st.td.getSize() * 1.3f);
                            designation.add(new Chunk(valeur(l.getService() != null
                                    ? l.getService().getNomService() : null), st.tdGras));
                            // designation.add(new Chunk("\nDu " + valeurDate(l.getDebutService())
                            //         + " au " + valeurDate(l.getFinService()), st.tdPetit));
                            faCorps(table, st, designation, Element.ALIGN_LEFT, zebra);
                            faCorps(table, st, new Phrase(formatMontant(l.getTarif()), st.td),
                                    Element.ALIGN_RIGHT, zebra);
                            faCorps(table, st, new Phrase("1", st.td), Element.ALIGN_CENTER, zebra);
                            faCorps(table, st, new Phrase(formatMontant(l.getTarif()), st.td),
                                    Element.ALIGN_RIGHT, zebra);
                            faCorps(table, st, new Phrase("-", st.td), Element.ALIGN_RIGHT, zebra);
                            faCorps(table, st, new Phrase(formatMontant(l.getTarif()), st.td),
                                    Element.ALIGN_RIGHT, zebra);
                            totalHt = totalHt.add(montantOuZero(l.getTarif()));
                        }
                    }
                    faSousTotal(table, st, "Sous-total Prestation", 3);
                    faSousTotal(table, st, formatMontant(totalHt), 1);
                    faSousTotal(table, st, "-", 1);
                    faSousTotal(table, st, formatMontant(totalHt), 1);
                    doc.add(table);

                    // ---- Calculs ----
                    BigDecimal tauxTva = lireTauxTva();
                    BigDecimal tva = totalHt.multiply(tauxTva)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal ttc = totalHt.add(tva);
                    BigDecimal avance = avanceRecue(f);
                    BigDecimal netAPayer = ttc.subtract(avance).max(BigDecimal.ZERO);

                    // ---- Totaux (colonne droite) ----
                    PdfPTable totaux = new PdfPTable(2);
                    totaux.setWidthPercentage(100);
                    totaux.setWidths(new float[]{5f, 4.6f});
                    faLigneTotaux(totaux, st, "TOTAL HT", totalHt, 0);
                    faLigneTotaux(totaux, st, "TVA" + (tauxTva.signum() > 0
                            ? " (" + tauxTva.stripTrailingZeros().toPlainString() + " %)" : ""), tva, 0);
                    faLigneTotaux(totaux, st, "TOTAL TTC", ttc, 1);
                    faLigneTotaux(totaux, st, "Avance reçue", avance, 0);
                    faLigneTotaux(totaux, st, "NET À PAYER", netAPayer, 2);

                    // ---- Montant en lettres + observations (colonne gauche) ----
                    PdfPTable gauche = new PdfPTable(1);
                    gauche.setWidthPercentage(100);
                    gauche.addCell(celluleLettres("Arrêtée la présente facture à la somme de",
                            MontantEnLettres.convertir(netAPayer.longValue()).toUpperCase(Locale.FRANCE)
                                    + " ARIARY"));
                    PdfPCell espace = celluleVide();
                    espace.setFixedHeight(st.ecart * 0.6f);
                    gauche.addCell(espace);

                    PdfPCell obs = new PdfPCell();
                    obs.setBorderColor(GRIS_BORDURE);
                    obs.setBorderWidth(0.5f);
                    obs.setPadding(st.padCarte);
                    obs.setMinimumHeight(st.hObservations);
                    obs.addElement(new Paragraph("OBSERVATIONS", st.carteTitre));
                    String commentaire = demande != null ? valeurTexte(demande.getCommentaire()) : "";
                    if (!commentaire.isEmpty()) {
                        Paragraph pc = new Paragraph(commentaire, st.petit);
                        pc.setSpacingBefore(2);
                        obs.addElement(pc);
                    }
                    gauche.addCell(obs);

                    PdfPTable bas = new PdfPTable(3);
                    bas.setWidthPercentage(100);
                    bas.setWidths(new float[]{5.4f, 0.35f, 4.25f});
                    bas.setSpacingBefore(st.ecart);
                    bas.setKeepTogether(true);
                    PdfPCell cg = new PdfPCell(gauche);
                    cg.setBorder(Rectangle.NO_BORDER);
                    cg.setPadding(0);
                    bas.addCell(cg);
                    bas.addCell(celluleVide());
                    PdfPCell cd = new PdfPCell(totaux);
                    cd.setBorder(Rectangle.NO_BORDER);
                    cd.setPadding(0);
                    bas.addCell(cd);
                    doc.add(bas);

                    // ---- Conditions de règlement ----
                    doc.add(faConditions(st));

                    // ---- Signatures ----
                    PdfPTable signatures = new PdfPTable(4);
                    signatures.setWidthPercentage(100);
                    signatures.setSpacingBefore(st.ecart);
                    signatures.setKeepTogether(true);
                    signatures.addCell(faSignature(st, "Client", null));
                    signatures.addCell(faSignature(st, "Responsable Traitement", "Dossiers Clients"));
                    signatures.addCell(faSignature(st, "Responsable Audit", "Contrôleur de Gestion"));
                    signatures.addCell(faSignature(st, "Manager", null));
                    for (int k = 0; k < 4; k++) {
                        PdfPCell vide = new PdfPCell(new Phrase(" ", st.petit));
                        vide.setBorderColor(GRIS_BORDURE);
                        vide.setBorderWidth(0.5f);
                        vide.setFixedHeight(st.hSignature);
                        signatures.addCell(vide);
                    }
                    doc.add(signatures);
                },
                "Erreur de génération de la facture proforma");
    }

    private byte[] genererFacture(String titreMeta, Contenu contenu, String messageErreur) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, MARGE_FACTURE, MARGE_FACTURE, 26, HAUTEUR_PIED_FACTURE);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Decor(false, "Facture proforma", true));
            doc.addTitle(titreMeta);
            doc.addAuthor(NOM_CABINET);
            doc.addCreator(NOM_CABINET);
            doc.open();
            contenu.ecrire(doc);
            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException(messageErreur, e);
        }
        return out.toByteArray();
    }

    private byte[] genererFiche(String titreMeta, Contenu contenu, String messageErreur) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document ficheDoc = new Document(PageSize.A4, MARGE_FACTURE, MARGE_FACTURE, 26, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(ficheDoc, out);
            writer.setPageEvent(new Decor(false, "Fiche de création", false, true));
            ficheDoc.addTitle(titreMeta);
            ficheDoc.addAuthor(NOM_CABINET);
            ficheDoc.addCreator(NOM_CABINET);
            ficheDoc.open();
            contenu.ecrire(ficheDoc);
            ficheDoc.close();
        } catch (Exception e) {
            throw new IllegalStateException(messageErreur, e);
        }
        return out.toByteArray();
    }


    // ==================================================================
    //  FICHE DE CRÉATION D'ENTREPRISE OU DE SOCIÉTÉ
    //  (reprend la fiche papier, design aligné sur la facture proforma)
    // ==================================================================

    public byte[] ficheCreation(DemandeService d,
                               com.cabex.backend.dossier.entity.DossierCreation dossier) {
        return genererFiche("Fiche de création " + valeur(d.getReferenceDemande()),
                doc -> construireFiche(doc, d, dossier, new StyleFacture(1f)),
                "Erreur de génération de la fiche de création");
    }

    private void construireFiche(Document doc, DemandeService d,
                                 com.cabex.backend.dossier.entity.DossierCreation dossier,
                                 StyleFacture st) {
        // ---- En-tête : logo + identité + titre ----
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.1f, 3.4f, 3f});
        t.addCell(celluleLogo(st.logo));
        PdfPCell identite = new PdfPCell();
        identite.setBorder(Rectangle.NO_BORDER);
        identite.setVerticalAlignment(Element.ALIGN_MIDDLE);
        identite.setPaddingLeft(6);
        identite.addElement(new Paragraph(marque(18f * st.tailleMeta / 11f)));
        Paragraph act = new Paragraph(ACTIVITE.toUpperCase(Locale.FRANCE), st.mention);
        act.setSpacingBefore(2);
        identite.addElement(act);
        t.addCell(identite);
        PdfPCell titre = new PdfPCell();
        titre.setBorder(Rectangle.NO_BORDER);
        titre.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pt = new Paragraph("FICHE DE CRÉATION", st.titre);
        pt.setAlignment(Element.ALIGN_RIGHT);
        titre.addElement(pt);
        Paragraph sous = new Paragraph("ENTREPRISE OU SOCIÉTÉ", st.sousTitre);
        sous.setAlignment(Element.ALIGN_RIGHT);
        titre.addElement(sous);
        t.addCell(titre);
        doc.add(t);
        doc.add(filet(3, st.ecart * 0.7f));

        // ---- Bandeau références ----
        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(st.ecart * 0.8f);
        meta.addCell(faMeta(st, "DEMANDE", valeur(d.getReferenceDemande()), ROUGE));
        meta.addCell(faMeta(st, "DATE", valeurDate(d.getDateDemande()), PRIMAIRE));
        String commercial = "-";
        if (d.getCommercial() != null) {
            commercial = d.getCommercial().getNomCommercial()
                    + " — N° IM : " + valeur(d.getCommercial().getImmatricule());
        }
        meta.addCell(faMeta(st, "COMMERCIAL", commercial, PRIMAIRE));
        doc.add(meta);

        String forme = code(dossier.getFormeJuridique());

        // ---- 1. Forme juridique ----
        PdfPCell b1 = faBoite(st);
        b1.addElement(new Paragraph("1. FORME JURIDIQUE", st.carteTitre));
        b1.addElement(ficheCases(st, new String[][]{
                {"INDIVIDUELLE", "Individuelle"},
                {"SARL", "SARL"},
                {"SARLU", "SARLU"},
                {"SA", "SA"},
                {"AUTRE", "Autre à préciser : " + valeurTexte(dossier.getFormeJuridiquePrecision())}
        }, forme));
        b1.addElement(ficheLigne(st, "Dénomination", valeurTexte(dossier.getDenomination())));
        b1.addElement(ficheLigne(st, "Siège social actuel",
                valeurTexte(dossier.getSiegeSocialActuel())));
        doc.add(boiteFiche(b1));

        // ---- 2. Associés (si société) ----
        PdfPCell b2 = faBoite(st);
        b2.addElement(new Paragraph("2. SI SOCIÉTÉ", st.carteTitre));
        b2.addElement(ficheLigne(st, "Capital social",
                formatMontant(dossier.getCapitalSocial()) + " Ariary"));
        b2.addElement(ficheLigne(st, "Nombre d'associés",
                valeurNombre(dossier.getNombreAssociesDeclare())));
        if (dossier.getAssocies() != null && !dossier.getAssocies().isEmpty()) {
            PdfPTable associes = new PdfPTable(4);
            associes.setWidthPercentage(100);
            associes.setSpacingBefore(4);
            ficheMiniTh(associes, st, "NOM DES ASSOCIÉS");
            ficheMiniTh(associes, st, "PART SOCIALE");
            ficheMiniTh(associes, st, "ADRESSE MAIL");
            ficheMiniTh(associes, st, "CONTACT");
            boolean zebra = false;
            for (com.cabex.backend.dossier.entity.DossierAssocie a : dossier.getAssocies()) {
                ficheMiniTd(associes, st, valeurTexte(a.getNom()), zebra);
                ficheMiniTd(associes, st, valeurTexte(a.getPartSociale()), zebra);
                ficheMiniTd(associes, st, valeurTexte(a.getEmail()), zebra);
                ficheMiniTd(associes, st, valeurTexte(a.getContact()), zebra);
                zebra = !zebra;
            }
            b2.addElement(associes);
        }
        b2.addElement(ficheLigne(st, "Nom du gérant", valeurTexte(dossier.getNomGerant())));
        b2.addElement(ficheLigne(st, "Fonction", valeurTexte(dossier.getFonctionGerant())));
        b2.addElement(ficheLigne(st, "",
                ficheCase(st, Boolean.TRUE.equals(dossier.getLocationGerance()))
                        + "  Location de gérance"));
        doc.add(boiteFiche(b2));

        // ---- 3. Secteur d'activité ----
        PdfPCell b3 = faBoite(st);
        b3.addElement(new Paragraph("3. SECTEUR D'ACTIVITÉ", st.carteTitre));
        b3.addElement(ficheLigne(st, "Secteur", valeurTexte(dossier.getSecteurActivite())));
        b3.addElement(ficheLigne(st, "Autorisation spéciale",
                valeurTexte(dossier.getAutorisationSpeciale())));
        doc.add(boiteFiche(b3));

        // ---- 4. Adresse du siège ----
        PdfPCell b4 = faBoite(st);
        b4.addElement(new Paragraph("4. ADRESSE DU SIÈGE ET DE L'ÉTABLISSEMENT", st.carteTitre));
        b4.addElement(ficheLigne(st, "Adresse", valeurTexte(dossier.getAdresseSiege())));
        b4.addElement(ficheLigne(st, "Ville", valeurTexte(dossier.getVille())));
        b4.addElement(ficheLigne(st, "Commune", valeurTexte(dossier.getCommune())));
        b4.addElement(ficheLigne(st, "District", valeurTexte(dossier.getDistrict())));
        b4.addElement(ficheLigne(st, "Région", valeurTexte(dossier.getRegion())));
        String hebergement = code(dossier.getTypeHebergement());
        b4.addElement(ficheLigne(st, "",
                ficheCase(st, estValeur(hebergement, "DOMICILE_GERANT"))
                        + "  Au domicile du gérant ou du propriétaire"
                        + "      " + ficheCase(st, estValeur(hebergement, "LOCATION_CABINET"))
                        + "  Location de siège au Cabinet Nakay"));
        b4.addElement(ficheLigne(st, "Autres", estValeur(hebergement, "AUTRE")
                ? valeurTexte(dossier.getHebergementPrecision()) : ""));
        b4.addElement(ficheLigne(st, "",
                ficheCase(st, dossier.getContratBail() != null)
                        + "  Possédez-vous un contrat de bail ?"
                        + (dossier.getContratBail() != null
                        ? (Boolean.TRUE.equals(dossier.getContratBail()) ? "  Oui" : "  Non") : "")
                        + (dossier.getContratBailDetail() != null
                        && !dossier.getContratBailDetail().isBlank()
                        ? " — " + dossier.getContratBailDetail() : "")));
        doc.add(boiteFiche(b4));

        // ---- 5. Objet social ----
        PdfPCell b5 = faBoite(st);
        b5.addElement(new Paragraph("5. PRINCIPALES ACTIVITÉS DE L'OBJET SOCIAL", st.carteTitre));
        b5.addElement(ficheLigne(st, "", valeurTexte(dossier.getObjetSocial())));
        b5.addElement(new Paragraph("DATE SOUHAITÉE POUR LE DÉBUT DE L'ACTIVITÉ", st.carteTitre));
        b5.addElement(ficheLigne(st, "Date", valeurDate(dossier.getDateSouhaiteeDebut())));
        String regime = code(dossier.getRegimeActivite());
        b5.addElement(ficheLigne(st, "",
                "Permanente " + ficheCase(st, estValeur(regime, "PERMANENTE"))
                        + "      Saisonnière " + ficheCase(st, estValeur(regime, "SAISONNIERE"))
                        + "      Ambulant " + ficheCase(st, estValeur(regime, "AMBULANT"))));
        b5.addElement(ficheLigne(st, "Activités exercées dans l'établissement ou succursale ou filiale",
                valeurTexte(dossier.getActivitesEtablissement())));
        doc.add(boiteFiche(b5));

        // ---- 6. Situation ----
        PdfPCell b6 = faBoite(st);
        b6.addElement(new Paragraph("6. OBSERVATIONS", st.carteTitre));
        b6.addElement(ficheLigne(st, "", valeurTexte(dossier.getObservations())));
        b6.addElement(ficheLigne(st, "Assujettissement à la TVA",
                valeurTexte(dossier.getAssujettissementTva())));
        b6.addElement(ficheLigne(st, "Avez-vous d'autre société ?",
                valeurTexte(dossier.getAutreSociete())));
        b6.addElement(ficheLigne(st, "Situation matrimoniale",
                valeurTexte(dossier.getSituationMatrimoniale())));
        b6.addElement(ficheLigne(st, "Régime d'imposition",
                valeurTexte(dossier.getRegimeImposition())));
        doc.add(boiteFiche(b6));

        // ---- Signatures ----
        PdfPTable signatures = new PdfPTable(4);
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(st.ecart);
        signatures.setKeepTogether(true);
        signatures.addCell(faSignature(st, "Le Client", null));
        signatures.addCell(faSignature(st, "Le Commercial", null));
        signatures.addCell(faSignature(st, "Le Responsable du dossier", null));
        signatures.addCell(faSignature(st, "Manager", null));
        for (int k = 0; k < 4; k++) {
            PdfPCell vide = new PdfPCell(new Phrase(" ", st.petit));
            vide.setBorderColor(GRIS_BORDURE);
            vide.setBorderWidth(0.5f);
            vide.setFixedHeight(st.hSignature);
            signatures.addCell(vide);
        }
        doc.add(signatures);
    }

    /** Enveloppe une carte dans un tableau mono-colonne (seul moyen de dessiner une cellule). */
    private PdfPTable boiteFiche(PdfPCell carte) {
        PdfPTable enveloppe = new PdfPTable(1);
        enveloppe.setWidthPercentage(100);
        enveloppe.setSpacingBefore(7);
        enveloppe.addCell(carte);
        return enveloppe;
    }

    /** Case cochée (✓) ou vide (○) façon fiche papier, design proforma. */
    private String ficheCase(StyleFacture st, boolean coche) {
        return coche ? "[X]" : "[ ]";
    }

    private Paragraph ficheLigne(StyleFacture st, String libelle, String valeur) {
        Paragraph p = new Paragraph();
        p.setLeading(st.petit.getSize() * 1.45f);
        p.setSpacingBefore(1.5f);
        if (libelle != null && !libelle.isEmpty()) {
            p.add(new Chunk(libelle + " : ", st.petitGras));
        }
        p.add(new Chunk(valeur == null || valeur.isEmpty() ? "—" : valeur, st.petit));
        return p;
    }

    private Paragraph ficheCases(StyleFacture st, String[][] options, String selection) {
        Paragraph p = new Paragraph();
        p.setLeading(st.petit.getSize() * 1.6f);
        p.setSpacingBefore(2);
        for (int i = 0; i < options.length; i++) {
            if (i > 0) {
                p.add(new Chunk("      ", st.petit));
            }
            p.add(new Chunk(ficheCase(st, estValeur(selection, options[i][0])) + "  "
                    + options[i][1], st.petit));
        }
        return p;
    }

    private boolean estValeur(String selection, String code) {
        return selection != null && selection.equals(code);
    }

    private String code(String valeur) {
        return valeur != null ? valeur.trim().toUpperCase(Locale.FRANCE) : "";
    }

    private String valeurNombre(Integer n) {
        return n != null ? n.toString() : "";
    }

    private String valeurNombre(BigDecimal montant) {
        return montant != null ? formatMontant(montant) : "";
    }

    private void ficheMiniTh(PdfPTable table, StyleFacture st, String texte) {
        PdfPCell c = new PdfPCell(new Phrase(texte, st.th));
        c.setBackgroundColor(PRIMAIRE);
        c.setPadding(4);
        table.addCell(c);
    }

    private void ficheMiniTd(PdfPTable table, StyleFacture st, String texte, boolean zebra) {
        PdfPCell c = new PdfPCell(new Phrase(texte.isEmpty() ? "—" : texte, st.petit));
        c.setPadding(4);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        if (zebra) {
            c.setBackgroundColor(ZEBRA);
        }
        table.addCell(c);
    }


    /** Ligne « LIBELLÉ : valeur » de la fiche standard (façon fiche papier). */
    private void ficheStdChamp(Document doc, String libelle, String valeur) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3);
        p.add(new Chunk(libelle + " : ", F_VALEUR_GRAS));
        p.add(new Chunk(valeur == null || valeur.isEmpty() ? "" : valeur, F_VALEUR));
        doc.add(p);
    }

    /** Cases [X]/[ ] cochées si le domaine contient le libellé (insensible à la casse). */
    private Paragraph ficheStdCases(String domaine, String... libelles) {
        String norme = domaine != null ? domaine.toLowerCase(Locale.FRANCE) : "";
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3);
        p.setLeading(F_VALEUR.getSize() * 1.6f);
        for (int i = 0; i < libelles.length; i++) {
            if (i > 0) {
                p.add(new Chunk("      ", F_VALEUR));
            }
            boolean coche = !norme.isEmpty()
                    && norme.contains(libelles[i].toLowerCase(Locale.FRANCE));
            p.add(new Chunk(coche ? "[X]  " + libelles[i] : "[ ]  " + libelles[i], F_VALEUR));
        }
        return p;
    }

    /** Durée d'existence calculée depuis la date de création de la société. */
    private String dureeExistence(Societe s) {
        if (s == null || s.getDateCreation() == null) {
            return "";
        }
        int ans = java.time.Period.between(s.getDateCreation(), LocalDate.now()).getYears();
        return ans + (ans > 1 ? " ans" : " an")
                + " (depuis le " + s.getDateCreation().format(DATE_FMT) + ")";
    }


    // ==================================================================
    //  SOCLE : création du document, en-tête / pied de page
    // ==================================================================

    @FunctionalInterface
    private interface Contenu {
        void ecrire(Document doc) throws Exception;
    }

    private byte[] generer(String libelleDocument, boolean enteteSurChaquePage,
                           String titreMeta, Contenu contenu, String messageErreur) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = enteteSurChaquePage
                ? new Document(PageSize.A4, 40, 40, 104, 62)
                : new Document(PageSize.A4, 36, 36, 34, 56);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Decor(enteteSurChaquePage, libelleDocument, false));
            doc.addTitle(titreMeta);
            doc.addAuthor(NOM_CABINET);
            doc.addCreator(NOM_CABINET);
            doc.open();
            contenu.ecrire(doc);
            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException(messageErreur, e);
        }
        return out.toByteArray();
    }

    /** Nom de marque bicolore, comme le logo : CAB / EX en vert, INET / CELLENT en rouge. */
    private static Phrase marque(float taille) {
        Font vert = font(taille, Font.BOLD, VERT_TEXTE);
        Font rouge = font(taille, Font.BOLD, ROUGE);
        Phrase p = new Phrase();
        p.add(new Chunk("CAB", vert));
        p.add(new Chunk("INET ", rouge));
        p.add(new Chunk("EX", vert));
        p.add(new Chunk("CELLENT", rouge));
        return p;
    }

    /** En-tête (logo + identité + date) et pied de page paginé. */
    private static class Decor extends PdfPageEventHelper {
        private final boolean avecEntete;
        private final String libelleDocument;
        /** Mode facture : pied ancré en bas (groupe, contacts, mentions légales), sans pagination. */
        private final boolean pied;
        /** Mode discret (fiche de création) : aucun pied de page dessiné. */
        private final boolean discret;
        private PdfTemplate total;
        private BaseFont bf;
        private Image logo;

        Decor(boolean avecEntete, String libelleDocument, boolean pied) {
            this(avecEntete, libelleDocument, pied, false);
        }

        Decor(boolean avecEntete, String libelleDocument, boolean pied, boolean discret) {
            this.avecEntete = avecEntete;
            this.libelleDocument = libelleDocument;
            this.pied = pied;
            this.discret = discret;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(24, 10);
            bf = F_MENTION.getCalculatedBaseFont(false);
            if (avecEntete) {
                logo = chargerLogo();
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (discret) {
                return;
            }
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            float gauche = document.leftMargin();
            float droite = page.getWidth() - document.rightMargin();

            if (pied) {
                PdfPTable bloc = piedFacture();
                bloc.setTotalWidth(droite - gauche);
                bloc.setLockedWidth(true);
                bloc.writeSelectedRows(0, -1, gauche, HAUTEUR_PIED_FACTURE - 10, cb);
                float milieu = (gauche + droite) / 2;
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("NIF : " + NIF + "    STAT : " + STAT + "    RCS : " + RCS, F_MENTION_GRAS),
                        milieu, 22, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(NOM_CABINET + "  ·  " + ADRESSE_SIEGE + "  ·  " + MAIL_ACCUEIL, F_MENTION),
                        milieu, 12, 0);
                return;
            }

            if (avecEntete) {
                float haut = page.getHeight() - 30;
                float x = gauche;
                if (logo != null) {
                    logo.scaleToFit(52, 52);
                    logo.setAbsolutePosition(gauche, haut - logo.getScaledHeight());
                    try {
                        cb.addImage(logo);
                    } catch (Exception e) {
                        // logo ignoré
                    }
                    x += logo.getScaledWidth() + 10;
                }
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, marque(16), x, haut - 24, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(ACTIVITE.toUpperCase(Locale.FRANCE), F_MENTION), x, haut - 37, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase(libelleDocument.toUpperCase(Locale.FRANCE), F_CARTE_TITRE),
                        droite, haut - 18, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase("Généré le " + LocalDate.now().format(DATE_FMT), F_MENTION),
                        droite, haut - 31, 0);

                // Double filet façon logo : vert sur toute la largeur, rouge sur le début
                float yFilet = haut - 60;
                cb.setLineWidth(2.5f);
                cb.setColorStroke(VERT_MARQUE);
                cb.moveTo(gauche, yFilet);
                cb.lineTo(droite, yFilet);
                cb.stroke();
                cb.setColorStroke(ROUGE);
                cb.moveTo(gauche, yFilet);
                cb.lineTo(gauche + 46, yFilet);
                cb.stroke();
            }

            // Pied de page
            cb.setColorStroke(GRIS_BORDURE);
            cb.setLineWidth(0.5f);
            cb.moveTo(gauche, 42);
            cb.lineTo(droite, 42);
            cb.stroke();

            if (avecEntete) {
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(NOM_CABINET + "  ·  " + ADRESSE_SIEGE + "  ·  " + MAIL_ACCUEIL, F_MENTION),
                        gauche, 30, 0);
            } else {
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase("NIF : " + NIF + "    STAT : " + STAT + "    RCS : " + RCS, F_MENTION_GRAS),
                        gauche, 30, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(NOM_CABINET + "  ·  " + ADRESSE_SIEGE + "  ·  " + MAIL_ACCUEIL, F_MENTION),
                        gauche, 20, 0);
            }
            String texte = "Page " + writer.getPageNumber() + " / ";
            float x = droite - 38;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(texte, F_MENTION), x, 30, 0);
            cb.addTemplate(total, x + bf.getWidthPoint(texte, 7), 30 - 2);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber() - 1), F_MENTION), 0, 2, 0);
        }
    }

    private static Image chargerLogo() {
        try (InputStream in = PdfGenerator.class.getResourceAsStream("/static/logo-cabinet-excellent.jpg")) {
            if (in != null) {
                return Image.getInstance(in.readAllBytes());
            }
        } catch (Exception e) {
            // Repli : pas de logo.
        }
        return null;
    }

    // ==================================================================
    //  BLOCS RÉUTILISABLES
    // ==================================================================

    private void titreDocument(Document doc, String titre, String reference) {
        Paragraph p = new Paragraph(titre.toUpperCase(Locale.FRANCE), F_TITRE);
        p.setSpacingAfter(2);
        doc.add(p);
        Paragraph r = new Paragraph(reference, F_SOUS_TITRE);
        r.setSpacingAfter(16);
        doc.add(r);
    }

    private void section(Document doc, String texte) {
        PdfPCell c = new PdfPCell(new Phrase(texte.toUpperCase(Locale.FRANCE), F_SECTION));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PRIMAIRE);
        c.setBorderWidth(1f);
        c.setPaddingLeft(0);
        c.setPaddingBottom(4);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(18);
        t.setSpacingAfter(8);
        t.addCell(c);
        doc.add(t);
    }

    /** Carte d'information : titre doré + paires libellé (petit) / valeur. */
    private PdfPCell carte(String titre, String[][] lignes) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(VERT_PALE);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(2.5f);
        c.setBorderColorLeft(VERT_MARQUE);
        c.setPadding(10);
        c.addElement(new Paragraph(titre.toUpperCase(Locale.FRANCE), F_CARTE_TITRE));
        for (String[] l : lignes) {
            Paragraph lab = new Paragraph(l[0].toUpperCase(Locale.FRANCE), F_LABEL);
            lab.setSpacingBefore(6);
            c.addElement(lab);
            c.addElement(new Paragraph(l[1], F_VALEUR_GRAS));
        }
        return c;
    }

    private PdfPCell kpi(String libelle, String valeur, boolean mis) {
        PdfPCell c = new PdfPCell();
        c.setBorder(mis ? Rectangle.NO_BORDER : Rectangle.LEFT);
        c.setBackgroundColor(mis ? PRIMAIRE : VERT_PALE);
        if (!mis) {
            c.setBorderWidthLeft(2.5f);
            c.setBorderColorLeft(VERT_MARQUE);
        }
        c.setPadding(10);
        c.addElement(new Paragraph(libelle, mis ? F_LABEL_BLANC : F_LABEL));
        Paragraph v = new Paragraph(valeur, mis ? F_KPI_BLANC : F_KPI);
        v.setSpacingBefore(3);
        c.addElement(v);
        return c;
    }

    /** Aligne plusieurs cellules sur une même ligne, séparées par un espace. */
    private void rangee(Document doc, float espace, PdfPCell... cellules) {
        int n = cellules.length;
        float[] largeurs = new float[2 * n - 1];
        for (int i = 0; i < n; i++) {
            largeurs[2 * i] = 10f;
            if (i < n - 1) {
                largeurs[2 * i + 1] = espace;
            }
        }
        PdfPTable t = new PdfPTable(largeurs.length);
        t.setWidthPercentage(100);
        t.setWidths(largeurs);
        t.setSpacingAfter(4);
        for (int i = 0; i < n; i++) {
            t.addCell(cellules[i]);
            if (i < n - 1) {
                PdfPCell vide = new PdfPCell(new Phrase(""));
                vide.setBorder(Rectangle.NO_BORDER);
                t.addCell(vide);
            }
        }
        doc.add(t);
    }

    private PdfPTable encartLettres(String intro, String montantLettres) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(12);
        t.addCell(celluleLettres(intro.trim(), montantLettres));
        return t;
    }

    // ---- Tableaux ----

    private PdfPCell base(Phrase contenu, int alignement) {
        PdfPCell c = new PdfPCell(contenu);
        c.setHorizontalAlignment(alignement);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(6);
        c.setPaddingBottom(7);
        c.setPaddingLeft(7);
        c.setPaddingRight(7);
        c.setUseAscender(true);
        c.setUseDescender(true);
        return c;
    }

    private void entete(PdfPTable table, String texte, int alignement) {
        PdfPCell c = base(new Phrase(texte, F_TH), alignement);
        c.setBackgroundColor(PRIMAIRE);
        table.addCell(c);
    }

    private void corps(PdfPTable table, String texte, int alignement, boolean zebra) {
        corps(table, new Phrase(texte, F_TD), alignement, zebra);
    }

    private void corps(PdfPTable table, Phrase contenu, int alignement, boolean zebra) {
        PdfPCell c = base(contenu, alignement);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        if (zebra) {
            c.setBackgroundColor(ZEBRA);
        }
        table.addCell(c);
    }

    private void celluleTotal(PdfPTable table, String texte, int alignement, int portee) {
        PdfPCell c = base(new Phrase(texte, F_TD_GRAS), alignement);
        c.setColspan(portee);
        c.setBackgroundColor(VERT_PALE);
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(PRIMAIRE);
        c.setBorderWidth(1f);
        table.addCell(c);
    }

    /** niveau 0 : ligne simple, 1 : TOTAL TTC (fond pâle + filets), 2 : NET À PAYER (bandeau vert foncé). */
    private void ligneTotaux(PdfPTable table, String libelle, BigDecimal montant, int niveau) {
        Font fl = niveau == 2 ? F_TD_GRAS_BLANC : (niveau == 1 ? F_TD_GRAS : F_TD);
        Font fm = niveau == 0 ? F_TD : (niveau == 1 ? F_TD_GRAS : F_TD_GRAS_BLANC);
        PdfPCell lib = base(new Phrase(libelle, fl), Element.ALIGN_LEFT);
        PdfPCell val = base(new Phrase(formatMontant(montant) + " Ar", fm), Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[]{lib, val}) {
            c.setPaddingTop(niveau == 2 ? 9 : 6);
            c.setPaddingBottom(niveau == 2 ? 9 : 7);
            if (niveau == 2) {
                c.setBackgroundColor(PRIMAIRE);
            } else {
                if (niveau == 1) {
                    c.setBackgroundColor(VERT_PALE);
                }
                c.setBorder(niveau == 1 ? Rectangle.TOP | Rectangle.BOTTOM : Rectangle.BOTTOM);
                c.setBorderColor(niveau == 1 ? PRIMAIRE : GRIS_BORDURE);
                c.setBorderWidth(niveau == 1 ? 1f : 0.5f);
            }
            table.addCell(c);
        }
    }

    // ---- Facture : blocs spécifiques ----

    private void faEntete(Document doc, Facture f, StyleFacture st) {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.1f, 3.4f, 3f});

        t.addCell(celluleLogo(st.logo));

        PdfPCell identite = new PdfPCell();
        identite.setBorder(Rectangle.NO_BORDER);
        identite.setVerticalAlignment(Element.ALIGN_MIDDLE);
        identite.setPaddingLeft(6);
        identite.addElement(new Paragraph(marque(18f * st.tailleMeta / 11f)));
        Paragraph act = new Paragraph(ACTIVITE.toUpperCase(Locale.FRANCE), st.mention);
        act.setSpacingBefore(2);
        identite.addElement(act);
        t.addCell(identite);

        PdfPCell titre = new PdfPCell();
        titre.setBorder(Rectangle.NO_BORDER);
        titre.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pt = new Paragraph("FACTURE", st.titre);
        pt.setAlignment(Element.ALIGN_RIGHT);
        titre.addElement(pt);
        Paragraph sous = new Paragraph("PROFORMA", st.sousTitre);
        sous.setAlignment(Element.ALIGN_RIGHT);
        titre.addElement(sous);
        t.addCell(titre);
        doc.add(t);

        doc.add(filet(3, st.ecart * 0.7f));

        // Bandeau numéro / date / demande
        DemandeService demande = f.getDemande();
        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(st.ecart * 0.8f);
        meta.addCell(faMeta(st, "N° FACTURE", valeur(f.getReferenceFacture()), ROUGE));
        meta.addCell(faMeta(st, "DATE", valeurDate(f.getCreatedAt() != null
                ? f.getCreatedAt().toLocalDate() : null), PRIMAIRE));
        meta.addCell(faMeta(st, "DEMANDE", valeur(demande != null ? demande.getReferenceDemande() : null),
                PRIMAIRE));
        doc.add(meta);
    }

    private PdfPCell faMeta(StyleFacture st, String libelle, String valeur, Color couleurValeur) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(VERT_PALE);
        c.setBorder(Rectangle.RIGHT);
        c.setBorderColorRight(Color.WHITE);
        c.setBorderWidthRight(3f);
        c.setPadding(st.padCarte - 1f);
        c.addElement(new Paragraph(libelle, F_LABEL));
        Paragraph v = new Paragraph(valeur, font(st.tailleMeta, Font.BOLD, couleurValeur));
        v.setSpacingBefore(1);
        c.addElement(v);
        return c;
    }

    /** Carte à lignes « libellé | valeur » sur une seule ligne : beaucoup plus compacte qu'empilée. */
    private PdfPCell faCarte(StyleFacture st, String titre, String[][] lignes) {
        PdfPTable interieur = new PdfPTable(2);
        interieur.setWidthPercentage(100);
        interieur.setWidths(new float[]{1.2f, 2.4f});

        PdfPCell t = new PdfPCell(new Phrase(titre.toUpperCase(Locale.FRANCE), st.carteTitre));
        t.setColspan(2);
        t.setBorder(Rectangle.NO_BORDER);
        t.setPadding(0);
        t.setPaddingBottom(st.ecart * 0.4f);
        interieur.addCell(t);

        for (String[] l : lignes) {
            PdfPCell lib = new PdfPCell(new Phrase(l[0], st.label));
            lib.setBorder(Rectangle.NO_BORDER);
            lib.setPadding(0);
            lib.setPaddingBottom(st.padV * 0.5f);
            interieur.addCell(lib);
            PdfPCell val = new PdfPCell(new Phrase(l[1], st.valeurGras));
            val.setBorder(Rectangle.NO_BORDER);
            val.setPadding(0);
            val.setPaddingBottom(st.padV * 0.5f);
            interieur.addCell(val);
        }

        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(VERT_PALE);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(2.5f);
        c.setBorderColorLeft(VERT_MARQUE);
        c.setPadding(st.padCarte);
        c.addElement(interieur);
        return c;
    }

    // ---- Facture : cellules du tableau ----

    private PdfPCell faCellule(StyleFacture st, Phrase contenu, int alignement) {
        PdfPCell c = new PdfPCell(contenu);
        c.setHorizontalAlignment(alignement);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(st.padV);
        c.setPaddingBottom(st.padV + 1f);
        c.setPaddingLeft(st.padH);
        c.setPaddingRight(st.padH);
        c.setUseAscender(true);
        c.setUseDescender(true);
        return c;
    }

    private void faTh(PdfPTable table, StyleFacture st, String texte, int alignement) {
        PdfPCell c = faCellule(st, new Phrase(texte, st.th), alignement);
        c.setBackgroundColor(PRIMAIRE);
        table.addCell(c);
    }

    private void faCorps(PdfPTable table, StyleFacture st, Phrase contenu, int alignement, boolean zebra) {
        PdfPCell c = faCellule(st, contenu, alignement);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        if (zebra) {
            c.setBackgroundColor(ZEBRA);
        }
        table.addCell(c);
    }

    private void faSousTotal(PdfPTable table, StyleFacture st, String texte, int portee) {
        PdfPCell c = faCellule(st, new Phrase(texte, st.tdGras), Element.ALIGN_RIGHT);
        c.setColspan(portee);
        c.setBackgroundColor(VERT_PALE);
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(PRIMAIRE);
        c.setBorderWidth(1f);
        table.addCell(c);
    }

    /** niveau 0 : ligne simple, 1 : TOTAL TTC (fond pâle + filets), 2 : NET À PAYER (bandeau vert foncé). */
    private void faLigneTotaux(PdfPTable table, StyleFacture st, String libelle, BigDecimal montant, int niveau) {
        Font fl = niveau == 2 ? st.tdBlanc : (niveau == 1 ? st.tdGras : st.td);
        Font fm = niveau == 0 ? st.td : (niveau == 1 ? st.tdGras : st.tdBlanc);
        PdfPCell lib = faCellule(st, new Phrase(libelle, fl), Element.ALIGN_LEFT);
        PdfPCell val = faCellule(st, new Phrase(formatMontant(montant) + " Ar", fm), Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[]{lib, val}) {
            if (niveau == 2) {
                c.setPaddingTop(st.padV * 1.5f);
                c.setPaddingBottom(st.padV * 1.5f + 1f);
                c.setBackgroundColor(PRIMAIRE);
            } else {
                if (niveau == 1) {
                    c.setBackgroundColor(VERT_PALE);
                }
                c.setBorder(niveau == 1 ? Rectangle.TOP | Rectangle.BOTTOM : Rectangle.BOTTOM);
                c.setBorderColor(niveau == 1 ? PRIMAIRE : GRIS_BORDURE);
                c.setBorderWidth(niveau == 1 ? 1f : 0.5f);
            }
            table.addCell(c);
        }
    }

    // ---- Facture : logo, règlement, signatures, pied ----

    private PdfPCell celluleLogo(float taille) {
        Image image = chargerLogo();
        if (image != null) {
            image.scaleToFit(taille, taille);
            PdfPCell c = new PdfPCell(image, true);
            c.setBorder(Rectangle.NO_BORDER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(2);
            return c;
        }
        return celluleVide();
    }

    private Paragraph faMode(StyleFacture st, String titre, String detail) {
        Paragraph p = new Paragraph();
        p.setLeading(st.petit.getSize() * 1.35f);
        p.setSpacingBefore(st.ecart * 0.4f);
        p.add(new Chunk("o", st.caseCocher));
        p.add(new Chunk("  " + titre, st.petitGras));
        if (detail != null) {
            p.add(new Chunk("\n      " + detail, st.mention));
        }
        return p;
    }

    private PdfPCell faBoite(StyleFacture st) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setPadding(st.padCarte);
        return c;
    }

    private PdfPTable faConditions(StyleFacture st) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1.45f});
        t.setSpacingBefore(st.ecart);
        t.setKeepTogether(true);

        PdfPCell banque = faBoite(st);
        banque.addElement(new Paragraph("COORDONNÉES BANCAIRES", st.carteTitre));
        for (int i = 0; i < COORDONNEES_BANCAIRES.length; i++) {
            Paragraph l = new Paragraph(COORDONNEES_BANCAIRES[i], i == 1 ? st.petitGras : st.petit);
            l.setLeading(st.petit.getSize() * 1.3f);
            l.setSpacingBefore(i == 0 ? st.ecart * 0.4f : 1.5f);
            banque.addElement(l);
        }
        t.addCell(banque);

        PdfPCell modes = faBoite(st);
        modes.addElement(new Paragraph("MODE DE RÈGLEMENT", st.carteTitre));
        modes.addElement(faMode(st, "Par chèque",
                "Banque ..................   N° chèque ............   Date ..........   Montant .........."));
        modes.addElement(faMode(st, "Par virement",
                "Banque ..................   Date ..........   Montant .........."));
        modes.addElement(faMode(st, "Par espèces", null));
        modes.addElement(faMode(st, "Par mobile money", null));
        t.addCell(modes);

        PdfPCell note = new PdfPCell(new Phrase("À payer dès la réception de la facture", st.note));
        note.setColspan(2);
        note.setHorizontalAlignment(Element.ALIGN_CENTER);
        note.setBackgroundColor(VERT_PALE);
        note.setBorderColor(GRIS_BORDURE);
        note.setBorderWidth(0.5f);
        note.setPadding(st.padV + 1f);
        t.addCell(note);
        return t;
    }

    private PdfPCell faSignature(StyleFacture st, String titre, String sousTitre) {
        Phrase p = new Phrase();
        p.setLeading(st.petit.getSize() * 1.25f);
        p.add(new Chunk(titre, st.petitGras));
        if (sousTitre != null) {
            p.add(new Chunk("\n" + sousTitre, st.mention));
        }
        PdfPCell c = new PdfPCell(p);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(VERT_PALE);
        c.setBorderColor(GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setPadding(st.padV);
        c.setMinimumHeight(st.petit.getSize() * 2.6f + st.padV);
        return c;
    }

    /** Pied de page de la facture : entités du groupe sur 4 colonnes puis contacts (dessiné en bas de page). */
    private static PdfPTable piedFacture() {
        PdfPTable t = new PdfPTable(4);

        Phrase titre = new Phrase();
        titre.add(new Chunk("SOCIÉTÉ GROUPE EXCELLENT", F_CARTE_TITRE));
        titre.add(new Chunk("     Capital : 222 126 000 Ar", F_PETIT_ITALIQUE));
        PdfPCell entete = new PdfPCell(titre);
        entete.setColspan(4);
        entete.setBorder(Rectangle.TOP);
        entete.setBorderColor(VERT_MARQUE);
        entete.setBorderWidth(1.5f);
        entete.setPaddingLeft(0);
        entete.setPaddingTop(5);
        entete.setPaddingBottom(3);
        t.addCell(entete);

        for (String entite : GROUPE) {
            PdfPCell e = new PdfPCell(new Phrase("•  " + entite, F_MENTION));
            e.setBorder(Rectangle.NO_BORDER);
            e.setPadding(1.5f);
            e.setPaddingLeft(0);
            t.addCell(e);
        }

        PdfPCell contacts = new PdfPCell(new Phrase(
                "Contact Tanà : " + CONTACT_TANA + "     Contact Majunga : " + CONTACT_MAJUNGA
                        + "     Mail : " + MAIL_NAKAY + "     Facebook : " + FACEBOOK, F_MENTION));
        contacts.setColspan(4);
        contacts.setHorizontalAlignment(Element.ALIGN_CENTER);
        contacts.setBorder(Rectangle.TOP);
        contacts.setBorderColor(GRIS_BORDURE);
        contacts.setBorderWidth(0.5f);
        contacts.setPaddingTop(5);
        t.addCell(contacts);
        return t;
    }

    /** Double filet façon logo : court segment rouge suivi d'un filet vert vif. */
    private PdfPTable filet(float avant, float apres) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 11f});
        t.setSpacingBefore(avant);
        t.setSpacingAfter(apres);
        t.addCell(celluleFilet(ROUGE));
        t.addCell(celluleFilet(VERT_MARQUE));
        return t;
    }

    private PdfPCell celluleFilet(Color couleur) {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(couleur);
        c.setBorderWidth(2.5f);
        c.setFixedHeight(4);
        return c;
    }

    private PdfPCell celluleVide() {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /** Montant en lettres : bandeau vert pâle à liseré vert vif. */
    private PdfPCell celluleLettres(String intro, String montantLettres) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(VERT_PALE);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(2.5f);
        c.setBorderColorLeft(VERT_MARQUE);
        c.setPadding(8);
        c.addElement(new Paragraph(intro.toUpperCase(Locale.FRANCE), F_LABEL));
        Paragraph m = new Paragraph(montantLettres, F_PETIT_GRAS);
        m.setSpacingBefore(3);
        c.addElement(m);
        return c;
    }

    private PdfPCell celluleSignature(String titre, String sousTitre) {
        Phrase p = new Phrase();
        p.setLeading(10);
        p.add(new Chunk(titre, F_PETIT_GRAS));
        if (sousTitre != null) {
            p.add(new Chunk("\n" + sousTitre, F_MENTION));
        }
        PdfPCell c = new PdfPCell(p);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(VERT_PALE);
        c.setBorderColor(GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setPadding(5);
        c.setMinimumHeight(28);
        return c;
    }

    private PdfPCell celluleEspaceSignature(float hauteur) {
        PdfPCell c = new PdfPCell(new Phrase(" ", F_PETIT));
        c.setBorderColor(GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setFixedHeight(hauteur);
        return c;
    }

    private Paragraph centre(Paragraph p) {
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    // ==================================================================
    //  CALCULS & FORMATAGE
    // ==================================================================

    private BigDecimal lireTauxTva() {
        try {
            String valeur = configurationService.lireValeur("TAUX_TVA", "0");
            return new BigDecimal(valeur.trim()).max(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** Avance déjà reçue : somme payée sur les échéances des lignes facturées. */
    private BigDecimal avanceRecue(Facture f) {
        BigDecimal avance = BigDecimal.ZERO;
        if (f.getLignes() == null) {
            return avance;
        }
        for (LigneService l : f.getLignes()) {
            if (l.getPlansPaiement() == null) {
                continue;
            }
            for (ProposPaiement plan : l.getPlansPaiement()) {
                if (plan.getEcheances() == null) {
                    continue;
                }
                for (EcheancePaiement e : plan.getEcheances()) {
                    avance = avance.add(montantOuZero(e.getMontantPaye()));
                }
            }
        }
        return avance;
    }

    private BigDecimal montantOuZero(BigDecimal montant) {
        return montant != null ? montant : BigDecimal.ZERO;
    }

    /** Format français : séparateur de milliers = espace insécable (compatible police Helvetica). */
    private DecimalFormat formatteur(int decimales) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator('\u00A0');
        symboles.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat(decimales == 0 ? "#,##0" : "#,##0.00", symboles);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }

    private String formatMontant(BigDecimal montant) {
        return montant == null ? "-" : formatteur(0).format(montant);
    }

    private String formatArgent(BigDecimal montant) {
        return montant == null ? "-" : formatteur(2).format(montant) + " Ar";
    }

    private String nomComplet(String prenom, String nom) {
        String r = (valeurTexte(prenom) + " " + valeurTexte(nom)).trim();
        return r.isEmpty() ? null : r;
    }

    private String valeur(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private String valeurTexte(String s) {
        return s == null || s.isBlank() ? "" : s;
    }

    private String valeurDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }
}