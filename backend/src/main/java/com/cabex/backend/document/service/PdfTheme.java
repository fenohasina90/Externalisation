package com.cabex.backend.document.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;

import java.awt.Color;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Charte graphique et identité du cabinet utilisées par tous les documents PDF.
 */
final class PdfTheme {

    // ------------------------------------------------------------------
    // Identité du cabinet (à modifier ici uniquement)
    // ------------------------------------------------------------------
    static final String NOM_CABINET = "CABINET EXCELLENT";
    static final String ACTIVITE = "Études et externalisation de services";
    static final String ADRESSE_SIEGE = "LOT II P 154 JC AVARADOHA, Antananarivo 101, Madagascar";
    static final String NIF = "6001199978";
    static final String STAT = "71102 11 2013 0 02016";
    static final String RCS = "2014A01518";
    static final String MAIL_ACCUEIL = "accueil.cabex@gmail.com";
    static final String FACEBOOK = "Cabinet Excellent";
    static final String CONTACT_TANA = "034 44 400 22";
    static final String CONTACT_MAJUNGA = "034 32 811 66";
    static final String MAIL_NAKAY = "cabinet.nakay@gmail.com";

    static final String[] COORDONNEES_BANCAIRES = {
            "BAOBAB BANQUE MADAGASCAR Ambodivona",
            "00013 - 00011 - 24798687313 - 64",
            "Au nom de : CABINET EXCELLENT",
            ADRESSE_SIEGE
    };

    static final String[] GROUPE = {
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
    static final Color PRIMAIRE = new Color(31, 98, 14);
    /** Vert vif du logo : filets et pastilles. */
    static final Color VERT_MARQUE = new Color(87, 203, 0);
    /** Vert du texte de marque (CAB / EX), un peu assombri pour la lisibilité sur blanc. */
    static final Color VERT_TEXTE = new Color(64, 168, 0);
    /** Rouge du logo (INET / CELLENT) : touche d'accent uniquement. */
    static final Color ROUGE = new Color(224, 32, 32);
    /** Vert très pâle (cartes, bandes de section). */
    static final Color VERT_PALE = new Color(238, 250, 226);
    /** Vert clair des libellés sur fond sombre. */
    static final Color VERT_CLAIR = new Color(212, 255, 176);
    /** Alternance de lignes des tableaux. */
    static final Color ZEBRA = new Color(247, 252, 243);
    static final Color GRIS_BORDURE = new Color(207, 220, 198);
    static final Color GRIS_TEXTE = new Color(98, 110, 94);
    static final Color TEXTE = new Color(32, 38, 32);

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ------------------------------------------------------------------
    // Mise en page
    // ------------------------------------------------------------------
    static final float MARGE_DOCUMENT = 32f;
    static final float HAUTEUR_PIED_FACTURE = 104f;

    // ------------------------------------------------------------------
    // Polices
    // ------------------------------------------------------------------
    static final Font F_TITRE = font(20, Font.BOLD, PRIMAIRE);
    static final Font F_SOUS_TITRE = font(10, Font.NORMAL, GRIS_TEXTE);
    static final Font F_SECTION = font(10, Font.BOLD, PRIMAIRE);
    static final Font F_CARTE_TITRE = font(8, Font.BOLD, PRIMAIRE);
    static final Font F_LABEL = font(7, Font.BOLD, GRIS_TEXTE);
    static final Font F_LABEL_BLANC = font(8, Font.BOLD, VERT_CLAIR);
    static final Font F_VALEUR = font(9.5f, Font.NORMAL, TEXTE);
    static final Font F_VALEUR_GRAS = font(9.5f, Font.BOLD, TEXTE);
    static final Font F_KPI = font(13, Font.BOLD, PRIMAIRE);
    static final Font F_KPI_BLANC = font(13, Font.BOLD, Color.WHITE);
    static final Font F_MONTANT_XL = font(24, Font.BOLD, Color.WHITE);
    static final Font F_TH = font(8, Font.BOLD, Color.WHITE);
    static final Font F_TD = font(9, Font.NORMAL, TEXTE);
    static final Font F_TD_GRAS = font(9, Font.BOLD, TEXTE);
    static final Font F_PETIT = font(8, Font.NORMAL, TEXTE);
    static final Font F_PETIT_GRAS = font(8, Font.BOLD, TEXTE);
    static final Font F_PETIT_ITALIQUE = font(8, Font.ITALIC, GRIS_TEXTE);
    static final Font F_MENTION = font(7, Font.NORMAL, GRIS_TEXTE);
    static final Font F_MENTION_GRAS = font(7, Font.BOLD, TEXTE);

    private PdfTheme() {
    }

    static Font font(float taille, int style, Color couleur) {
        return new Font(Font.HELVETICA, taille, style, couleur);
    }

    /** Nom de marque bicolore, comme le logo : CAB / EX en vert, INET / CELLENT en rouge. */
    static Phrase marque(float taille) {
        Font vert = font(taille, Font.BOLD, VERT_TEXTE);
        Font rouge = font(taille, Font.BOLD, ROUGE);
        Phrase p = new Phrase();
        p.add(new Chunk("CAB", vert));
        p.add(new Chunk("INET ", rouge));
        p.add(new Chunk("EX", vert));
        p.add(new Chunk("CELLENT", rouge));
        return p;
    }

    static Image chargerLogo() {
        try (InputStream in = PdfTheme.class.getResourceAsStream("/static/logo-cabinet-excellent.jpg")) {
            if (in != null) {
                return Image.getInstance(in.readAllBytes());
            }
        } catch (Exception e) {
            // Repli : pas de logo.
        }
        return null;
    }

    static BaseFont baseFontMention() {
        return F_MENTION.getCalculatedBaseFont(false);
    }

    static String upper(String texte) {
        return texte.toUpperCase(Locale.FRANCE);
    }
}
