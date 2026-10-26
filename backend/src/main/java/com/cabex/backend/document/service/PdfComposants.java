package com.cabex.backend.document.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;

/**
 * Blocs d'affichage réutilisables par tous les documents PDF
 * (titres, cartes, KPI, tableaux, signatures, filets).
 */
final class PdfComposants {

    private PdfComposants() {
    }

    // ------------------------------------------------------------------
    // Titres et sections
    // ------------------------------------------------------------------

    static void titreDocument(Document doc, String titre, String reference) {
        Paragraph p = new Paragraph(PdfTheme.upper(titre), PdfTheme.F_TITRE);
        p.setSpacingAfter(2);
        doc.add(p);
        Paragraph r = new Paragraph(reference, PdfTheme.F_SOUS_TITRE);
        r.setSpacingAfter(16);
        doc.add(r);
    }

    static void section(Document doc, String texte) {
        PdfPCell c = new PdfPCell(new Phrase(PdfTheme.upper(texte), PdfTheme.F_SECTION));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.PRIMAIRE);
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

    /**
     * En-tête compact des documents style facture : logo, identité du cabinet et
     * titre du document aligné à droite.
     */
    static void enteteDocument(Document doc, PdfStyleFacture st, String titre, String sousTitre) {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.1f, 3.4f, 3f});
        t.addCell(celluleLogo(st.logo));

        PdfPCell identite = new PdfPCell();
        identite.setBorder(Rectangle.NO_BORDER);
        identite.setVerticalAlignment(Element.ALIGN_MIDDLE);
        identite.setPaddingLeft(6);
        identite.addElement(new Paragraph(PdfTheme.marque(18f * st.tailleMeta / 11f)));
        Paragraph activite = new Paragraph(PdfTheme.upper(PdfTheme.ACTIVITE), st.mention);
        activite.setSpacingBefore(2);
        identite.addElement(activite);
        t.addCell(identite);

        PdfPCell titreCell = new PdfPCell();
        titreCell.setBorder(Rectangle.NO_BORDER);
        titreCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph principal = new Paragraph(titre, st.titre);
        principal.setAlignment(Element.ALIGN_RIGHT);
        titreCell.addElement(principal);
        Paragraph sous = new Paragraph(sousTitre, st.sousTitre);
        sous.setAlignment(Element.ALIGN_RIGHT);
        titreCell.addElement(sous);
        t.addCell(titreCell);
        doc.add(t);

        doc.add(filet(3, st.ecart * 0.7f));
    }

    // ------------------------------------------------------------------
    // Cartes et indicateurs
    // ------------------------------------------------------------------

    /** Carte d'information : titre + paires libellé (petit) / valeur. */
    static PdfPCell carte(String titre, String[][] lignes) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(2.5f);
        c.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        c.setPadding(10);
        c.addElement(new Paragraph(PdfTheme.upper(titre), PdfTheme.F_CARTE_TITRE));
        for (String[] ligne : lignes) {
            Paragraph label = new Paragraph(PdfTheme.upper(ligne[0]), PdfTheme.F_LABEL);
            label.setSpacingBefore(6);
            c.addElement(label);
            c.addElement(new Paragraph(ligne[1], PdfTheme.F_VALEUR_GRAS));
        }
        return c;
    }

    static PdfPCell kpi(String libelle, String valeur, boolean misEnAvant) {
        PdfPCell c = new PdfPCell();
        c.setBorder(misEnAvant ? Rectangle.NO_BORDER : Rectangle.LEFT);
        c.setBackgroundColor(misEnAvant ? PdfTheme.PRIMAIRE : PdfTheme.VERT_PALE);
        if (!misEnAvant) {
            c.setBorderWidthLeft(2.5f);
            c.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        }
        c.setPadding(10);
        c.addElement(new Paragraph(libelle,
                misEnAvant ? PdfTheme.F_LABEL_BLANC : PdfTheme.F_LABEL));
        Paragraph v = new Paragraph(valeur,
                misEnAvant ? PdfTheme.F_KPI_BLANC : PdfTheme.F_KPI);
        v.setSpacingBefore(3);
        c.addElement(v);
        return c;
    }

    /** Aligne plusieurs cellules sur une même ligne, séparées par un espace. */
    static void rangee(Document doc, float espace, PdfPCell... cellules) {
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
                t.addCell(celluleVide());
            }
        }
        doc.add(t);
    }

    static PdfPTable encartLettres(String intro, String montantLettres) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(12);
        t.addCell(celluleLettres(intro.trim(), montantLettres));
        return t;
    }

    // ------------------------------------------------------------------
    // Tableaux (polices fixes)
    // ------------------------------------------------------------------

    static PdfPCell base(Phrase contenu, int alignement) {
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

    static void entete(PdfPTable table, String texte, int alignement) {
        PdfPCell c = base(new Phrase(texte, PdfTheme.F_TH), alignement);
        c.setBackgroundColor(PdfTheme.PRIMAIRE);
        table.addCell(c);
    }

    static void corps(PdfPTable table, String texte, int alignement, boolean zebra) {
        corps(table, new Phrase(texte, PdfTheme.F_TD), alignement, zebra);
    }

    static void corps(PdfPTable table, Phrase contenu, int alignement, boolean zebra) {
        PdfPCell c = base(contenu, alignement);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        if (zebra) {
            c.setBackgroundColor(PdfTheme.ZEBRA);
        }
        table.addCell(c);
    }

    static void celluleTotal(PdfPTable table, String texte, int alignement, int portee) {
        PdfPCell c = base(new Phrase(texte, PdfTheme.F_TD_GRAS), alignement);
        c.setColspan(portee);
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(PdfTheme.PRIMAIRE);
        c.setBorderWidth(1f);
        table.addCell(c);
    }

    // ------------------------------------------------------------------
    // Tableaux (style facture / fiche)
    // ------------------------------------------------------------------

    static PdfPCell faCellule(PdfStyleFacture st, Phrase contenu, int alignement) {
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

    static void faTh(PdfPTable table, PdfStyleFacture st, String texte, int alignement) {
        PdfPCell c = faCellule(st, new Phrase(texte, st.th), alignement);
        c.setBackgroundColor(PdfTheme.PRIMAIRE);
        table.addCell(c);
    }

    static void faCorps(PdfPTable table, PdfStyleFacture st, Phrase contenu,
                        int alignement, boolean zebra) {
        PdfPCell c = faCellule(st, contenu, alignement);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        if (zebra) {
            c.setBackgroundColor(PdfTheme.ZEBRA);
        }
        table.addCell(c);
    }

    static void faSousTotal(PdfPTable table, PdfStyleFacture st, String texte, int portee) {
        PdfPCell c = faCellule(st, new Phrase(texte, st.tdGras), Element.ALIGN_RIGHT);
        c.setColspan(portee);
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(PdfTheme.PRIMAIRE);
        c.setBorderWidth(1f);
        table.addCell(c);
    }

    /**
     * Ligne de totaux : niveau 0 simple, 1 TOTAL TTC (fond pâle + filets),
     * 2 NET À PAYER (bandeau vert foncé).
     */
    static void faLigneTotaux(PdfPTable table, PdfStyleFacture st, String libelle,
                              String montant, int niveau) {
        Font fl = niveau == 2 ? st.tdBlanc : (niveau == 1 ? st.tdGras : st.td);
        Font fm = niveau == 0 ? st.td : (niveau == 1 ? st.tdGras : st.tdBlanc);
        PdfPCell lib = faCellule(st, new Phrase(libelle, fl), Element.ALIGN_LEFT);
        PdfPCell val = faCellule(st, new Phrase(montant + " Ar", fm), Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[]{lib, val}) {
            if (niveau == 2) {
                c.setPaddingTop(st.padV * 1.5f);
                c.setPaddingBottom(st.padV * 1.5f + 1f);
                c.setBackgroundColor(PdfTheme.PRIMAIRE);
            } else {
                if (niveau == 1) {
                    c.setBackgroundColor(PdfTheme.VERT_PALE);
                }
                c.setBorder(niveau == 1 ? Rectangle.TOP | Rectangle.BOTTOM : Rectangle.BOTTOM);
                c.setBorderColor(niveau == 1 ? PdfTheme.PRIMAIRE : PdfTheme.GRIS_BORDURE);
                c.setBorderWidth(niveau == 1 ? 1f : 0.5f);
            }
            table.addCell(c);
        }
    }

    // ------------------------------------------------------------------
    // Cartes et boîtes (style facture / fiche)
    // ------------------------------------------------------------------

    /** Carte à lignes « libellé | valeur » sur une seule ligne : plus compacte qu'empilée. */
    static PdfPCell faCarte(PdfStyleFacture st, String titre, String[][] lignes) {
        PdfPTable interieur = new PdfPTable(2);
        interieur.setWidthPercentage(100);
        interieur.setWidths(new float[]{1.2f, 2.4f});

        PdfPCell t = new PdfPCell(new Phrase(PdfTheme.upper(titre), st.carteTitre));
        t.setColspan(2);
        t.setBorder(Rectangle.NO_BORDER);
        t.setPadding(0);
        t.setPaddingBottom(st.ecart * 0.4f);
        interieur.addCell(t);

        for (String[] ligne : lignes) {
            interieur.addCell(champSansBordure(st, ligne[0], st.label));
            interieur.addCell(champSansBordure(st, ligne[1], st.valeurGras));
        }

        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(2.5f);
        c.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        c.setPadding(st.padCarte);
        c.addElement(interieur);
        return c;
    }

    private static PdfPCell champSansBordure(PdfStyleFacture st, String texte,
                                             Font police) {
        PdfPCell c = new PdfPCell(new Phrase(texte, police));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        c.setPaddingBottom(st.padV * 0.5f);
        return c;
    }

    static PdfPCell faMeta(PdfStyleFacture st, String libelle, String valeur, Color couleurValeur) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorder(Rectangle.RIGHT);
        c.setBorderColorRight(Color.WHITE);
        c.setBorderWidthRight(3f);
        c.setPadding(st.padCarte - 1f);
        c.addElement(new Paragraph(libelle, PdfTheme.F_LABEL));
        Paragraph v = new Paragraph(valeur, PdfTheme.font(st.tailleMeta,
                Font.BOLD, couleurValeur));
        v.setSpacingBefore(1);
        c.addElement(v);
        return c;
    }

    static PdfPCell faBoite(PdfStyleFacture st) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setPadding(st.padCarte);
        return c;
    }

    static PdfPCell celluleLogo(float taille) {
        Image image = PdfTheme.chargerLogo();
        if (image == null) {
            return celluleVide();
        }
        image.scaleToFit(taille, taille);
        PdfPCell c = new PdfPCell(image, true);
        c.setBorder(Rectangle.NO_BORDER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(2);
        return c;
    }

    // ------------------------------------------------------------------
    // Signatures
    // ------------------------------------------------------------------

    static PdfPCell faSignature(PdfStyleFacture st, String titre, String sousTitre) {
        Phrase p = new Phrase();
        p.setLeading(st.petit.getSize() * 1.25f);
        p.add(new Chunk(titre, st.petitGras));
        if (sousTitre != null) {
            p.add(new Chunk("\n" + sousTitre, st.mention));
        }
        PdfPCell c = new PdfPCell(p);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setPadding(st.padV);
        c.setMinimumHeight(st.petit.getSize() * 2.6f + st.padV);
        return c;
    }

    /**
     * Tableau de signatures (titres puis zones vierges) au style facture.
     *
     * @param signataires couples {titre, sous-titre éventuel}
     */
    static PdfPTable signaturesStyle(PdfStyleFacture st, float hauteur, String[]... signataires) {
        PdfPTable t = new PdfPTable(signataires.length);
        t.setWidthPercentage(100);
        t.setKeepTogether(true);
        for (String[] signataire : signataires) {
            t.addCell(faSignature(st, signataire[0],
                    signataire.length > 1 ? signataire[1] : null));
        }
        for (int i = 0; i < signataires.length; i++) {
            PdfPCell vide = new PdfPCell(new Phrase(" ", st.petit));
            vide.setBorderColor(PdfTheme.GRIS_BORDURE);
            vide.setBorderWidth(0.5f);
            vide.setFixedHeight(hauteur);
            t.addCell(vide);
        }
        return t;
    }

    static PdfPCell celluleSignature(String titre, String sousTitre) {
        Phrase p = new Phrase();
        p.setLeading(10);
        p.add(new Chunk(titre, PdfTheme.F_PETIT_GRAS));
        if (sousTitre != null) {
            p.add(new Chunk("\n" + sousTitre, PdfTheme.F_MENTION));
        }
        PdfPCell c = new PdfPCell(p);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setPadding(5);
        c.setMinimumHeight(28);
        return c;
    }

    static PdfPCell celluleEspaceSignature(float hauteur) {
        PdfPCell c = new PdfPCell(new Phrase(" ", PdfTheme.F_PETIT));
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setFixedHeight(hauteur);
        return c;
    }

    // ------------------------------------------------------------------
    // Divers
    // ------------------------------------------------------------------

    /** Double filet façon logo : court segment rouge suivi d'un filet vert vif. */
    static PdfPTable filet(float avant, float apres) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 11f});
        t.setSpacingBefore(avant);
        t.setSpacingAfter(apres);
        t.addCell(celluleFilet(PdfTheme.ROUGE));
        t.addCell(celluleFilet(PdfTheme.VERT_MARQUE));
        return t;
    }

    private static PdfPCell celluleFilet(Color couleur) {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(couleur);
        c.setBorderWidth(2.5f);
        c.setFixedHeight(4);
        return c;
    }

    /** Enveloppe une carte dans un tableau mono-colonne (seul moyen de dessiner une cellule). */
    static PdfPTable boiteFiche(PdfPCell carte) {
        PdfPTable enveloppe = new PdfPTable(1);
        enveloppe.setWidthPercentage(100);
        enveloppe.setSpacingBefore(8);
        enveloppe.addCell(carte);
        return enveloppe;
    }

    static PdfPCell celluleVide() {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /** Montant en lettres : bandeau vert pâle à liseré vert vif. */
    static PdfPCell celluleLettres(String intro, String montantLettres) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(PdfTheme.VERT_PALE);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(2.5f);
        c.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        c.setPadding(8);
        c.addElement(new Paragraph(PdfTheme.upper(intro), PdfTheme.F_LABEL));
        Paragraph m = new Paragraph(montantLettres, PdfTheme.F_PETIT_GRAS);
        m.setSpacingBefore(3);
        c.addElement(m);
        return c;
    }
}
