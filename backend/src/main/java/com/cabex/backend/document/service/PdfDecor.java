package com.cabex.backend.document.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.time.LocalDate;

/**
 * En-tête et pied de page des documents PDF.
 */
final class PdfDecor extends PdfPageEventHelper {

    enum Mode {
        /** En-tête complet (logo, identité, document, date) + pied paginé. */
        ENTETE,
        /** Pied facture ancré en bas de page (groupe, contacts, mentions), sans pagination. */
        FACTURE,
        /** Facturation client : en-tête (logo + « Facturation du client <nom> ») répété sur chaque page, pied paginé avec date de génération. */
        FACTURATION,
        /** Aucun en-tête ni pied. */
        SANS_PIED
    }

    private static final float HAUTEUR_ENTETE = 30f;
    private static final float LARGEUR_FILET_ROUGE = 46f;

    private final Mode mode;
    private final String libelleDocument;
    private PdfTemplate total;
    private BaseFont policeMention;
    private Image logo;

    PdfDecor(Mode mode, String libelleDocument) {
        this.mode = mode;
        this.libelleDocument = libelleDocument;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        total = writer.getDirectContent().createTemplate(24, 10);
        policeMention = PdfTheme.baseFontMention();
        if (mode == Mode.ENTETE || mode == Mode.FACTURATION) {
            logo = PdfTheme.chargerLogo();
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (mode == Mode.SANS_PIED) {
            return;
        }
        PdfContentByte cb = writer.getDirectContent();
        Rectangle page = document.getPageSize();
        float gauche = document.leftMargin();
        float droite = page.getWidth() - document.rightMargin();

        if (mode == Mode.FACTURE) {
            dessinerPiedFacture(cb, gauche, droite);
            return;
        }
        if (mode == Mode.FACTURATION) {
            dessinerEnteteFacturation(cb, page, gauche, droite);
            dessinerPiedFacturation(cb, writer, gauche, droite);
            return;
        }
        if (mode == Mode.ENTETE) {
            dessinerEntete(cb, page, gauche, droite);
        }
        dessinerPied(cb, writer, gauche, droite);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                new Phrase(String.valueOf(writer.getPageNumber() - 1), PdfTheme.F_MENTION), 0, 2, 0);
    }

    private void dessinerPiedFacture(PdfContentByte cb, float gauche, float droite) {
        PdfPTable bloc = PdfFactureComposants.piedFacture();
        bloc.setTotalWidth(droite - gauche);
        bloc.setLockedWidth(true);
        bloc.writeSelectedRows(0, -1, gauche, PdfTheme.HAUTEUR_PIED_FACTURE - 10, cb);

        float milieu = (gauche + droite) / 2;
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, mentionsLegales(), milieu, 22, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, coordonneesCabinet(), milieu, 12, 0);
    }

    private void dessinerEntete(PdfContentByte cb, Rectangle page, float gauche, float droite) {
        float haut = page.getHeight() - HAUTEUR_ENTETE;
        float x = gauche;
        if (logo != null) {
            logo.scaleToFit(52, 52);
            logo.setAbsolutePosition(gauche, haut - logo.getScaledHeight());
            try {
                cb.addImage(logo);
            } catch (Exception e) {
                // Logo ignoré si son insertion échoue.
            }
            x += logo.getScaledWidth() + 10;
        }
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, PdfTheme.marque(16), x, haut - 24, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(PdfTheme.upper(PdfTheme.ACTIVITE), PdfTheme.F_MENTION), x, haut - 37, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase(PdfTheme.upper(libelleDocument), PdfTheme.F_CARTE_TITRE), droite, haut - 18, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase("Généré le " + LocalDate.now().format(PdfTheme.DATE_FMT), PdfTheme.F_MENTION),
                droite, haut - 31, 0);

        dessinerFilets(cb, gauche, droite, haut - 60);
    }

    /** En-tête de la facturation client (logo + « Facturation du client <nom> »). */
    private void dessinerEnteteFacturation(PdfContentByte cb, Rectangle page,
                                          float gauche, float droite) {
        float haut = page.getHeight();
        float x = gauche;
        if (logo == null) {
            logo = PdfTheme.chargerLogo();
        }
        if (logo != null) {
            logo.scaleToFit(44, 44);
            logo.setAbsolutePosition(gauche, haut - 14 - logo.getScaledHeight());
            try {
                cb.addImage(logo);
            } catch (Exception e) {
                // Logo ignoré si son insertion échoue.
            }
            x += logo.getScaledWidth() + 10;
        }
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(PdfTheme.upper("Facturation du client"), PdfTheme.F_MENTION),
                x, haut - 22, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(libelleDocument, PdfTheme.font(15, Font.BOLD, PdfTheme.PRIMAIRE)),
                x, haut - 39, 0);
    }

    /** Pied de la facturation client (identité + date de génération à droite). */
    private void dessinerPiedFacturation(PdfContentByte cb, PdfWriter writer,
                                         float gauche, float droite) {
        cb.setColorStroke(PdfTheme.GRIS_BORDURE);
        cb.setLineWidth(0.5f);
        cb.moveTo(gauche, 38);
        cb.lineTo(droite, 38);
        cb.stroke();

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, coordonneesCabinet(), gauche, 27, 0);

        String pageTexte = "Page " + writer.getPageNumber() + " /";
        float totalWidth = policeMention.getWidthPoint(pageTexte, 7) + 14;
        // « Généré le … » puis « Page y / N » sur la même ligne, à droite
        String genereTexte = "Généré le " + LocalDate.now().format(PdfTheme.DATE_FMT) + "  ·  ";
        float x = droite - totalWidth - policeMention.getWidthPoint(genereTexte, 7) - 6;
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(genereTexte, PdfTheme.F_MENTION), x, 27, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(pageTexte, PdfTheme.F_MENTION), x + policeMention.getWidthPoint(genereTexte, 7), 27, 0);
        cb.addTemplate(total,
                x + policeMention.getWidthPoint(genereTexte, 7) + policeMention.getWidthPoint(pageTexte, 7), 27 - 2);
    }

    /** Double filet façon logo : vert sur toute la largeur, rouge sur le début. */
    private void dessinerFilets(PdfContentByte cb, float gauche, float droite, float y) {
        cb.setLineWidth(2.5f);
        cb.setColorStroke(PdfTheme.VERT_MARQUE);
        cb.moveTo(gauche, y);
        cb.lineTo(droite, y);
        cb.stroke();
        cb.setColorStroke(PdfTheme.ROUGE);
        cb.moveTo(gauche, y);
        cb.lineTo(gauche + LARGEUR_FILET_ROUGE, y);
        cb.stroke();
    }

    private void dessinerPied(PdfContentByte cb, PdfWriter writer, float gauche, float droite) {
        cb.setColorStroke(PdfTheme.GRIS_BORDURE);
        cb.setLineWidth(0.5f);
        cb.moveTo(gauche, 42);
        cb.lineTo(droite, 42);
        cb.stroke();

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, coordonneesCabinet(), gauche, 30, 0);

        String texte = "Page " + writer.getPageNumber() + " / ";
        float x = droite - 38;
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(texte, PdfTheme.F_MENTION), x, 30, 0);
        cb.addTemplate(total, x + policeMention.getWidthPoint(texte, 7), 30 - 2);
    }

    private static Phrase mentionsLegales() {
        return new Phrase("NIF : " + PdfTheme.NIF + "    STAT : " + PdfTheme.STAT
                + "    RCS : " + PdfTheme.RCS, PdfTheme.F_MENTION_GRAS);
    }

    private static Phrase coordonneesCabinet() {
        return new Phrase(PdfTheme.NOM_CABINET + "  ·  " + PdfTheme.ADRESSE_SIEGE
                + "  ·  " + PdfTheme.MAIL_ACCUEIL, PdfTheme.F_MENTION);
    }
}
