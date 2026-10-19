package com.cabex.backend.document.service;

import com.lowagie.text.Font;

import java.awt.Color;

/**
 * Dimensions et polices des documents compacts (facture proforma, fiche de création),
 * dérivées d'une échelle de compacité (1 = mise en page confort).
 */
final class PdfStyleFacture {

    /**
     * Échelles essayées tour à tour : le document est d'abord généré en mise en page
     * « confort » ; s'il déborde, il est regénéré plus compacte jusqu'à tenir sur une page.
     */
    static final float[] ECHELLES = {1f, 0.92f, 0.84f, 0.76f, 0.68f, 0.60f, 0.52f};

    final Font th;
    final Font td;
    final Font tdGras;
    final Font tdBlanc;
    final Font label;
    final Font valeurGras;
    final Font petit;
    final Font petitGras;
    final Font mention;
    final Font carteTitre;
    final Font section;
    final Font note;
    final Font titre;
    final Font sousTitre;
    final Font caseCocher;
    final float tailleMeta;
    final float padV;
    final float padH;
    final float padCarte;
    final float ecart;
    final float hSignature;
    final float hObservations;
    final float logo;

    PdfStyleFacture(float echelle) {
        // Les polices rétrécissent deux fois moins vite que les espacements (lisibilité).
        float f = 1f - (1f - echelle) / 2f;
        th = PdfTheme.font(8f * f, Font.BOLD, Color.WHITE);
        td = PdfTheme.font(9f * f, Font.NORMAL, PdfTheme.TEXTE);
        tdGras = PdfTheme.font(9f * f, Font.BOLD, PdfTheme.TEXTE);
        tdBlanc = PdfTheme.font(10f * f, Font.BOLD, Color.WHITE);
        label = PdfTheme.font(7.5f * f, Font.NORMAL, PdfTheme.GRIS_TEXTE);
        valeurGras = PdfTheme.font(9f * f, Font.BOLD, PdfTheme.TEXTE);
        petit = PdfTheme.font(8f * f, Font.NORMAL, PdfTheme.TEXTE);
        petitGras = PdfTheme.font(8f * f, Font.BOLD, PdfTheme.TEXTE);
        mention = PdfTheme.font(7f * f, Font.NORMAL, PdfTheme.GRIS_TEXTE);
        carteTitre = PdfTheme.font(8f * f, Font.BOLD, PdfTheme.PRIMAIRE);
        section = PdfTheme.font(9f * f, Font.BOLD, PdfTheme.PRIMAIRE);
        note = PdfTheme.font(9f * f, Font.BOLDITALIC, PdfTheme.PRIMAIRE);
        titre = PdfTheme.font(26f * f, Font.BOLD, PdfTheme.PRIMAIRE);
        sousTitre = PdfTheme.font(10f * f, Font.BOLD, PdfTheme.ROUGE);
        caseCocher = new Font(Font.ZAPFDINGBATS, 9f * f, Font.NORMAL, PdfTheme.PRIMAIRE);
        tailleMeta = 11f * f;
        padV = 5.5f * echelle;
        padH = 7f;
        padCarte = 9f * echelle + 1f;
        ecart = 11f * echelle + 3f;
        hSignature = 52f * echelle;
        hObservations = 46f * echelle;
        logo = 62f * f;
    }
}
