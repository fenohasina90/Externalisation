package com.cabex.backend.document.service;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Création du document PDF et de son décor (en-tête / pied de page).
 */
final class PdfDocument {

    /** Mise en page d'un document, avec ses marges et son décor. */
    enum MiseEnPage {
        /** Documents d'information : en-tête complet et pied paginé. */
        DOCUMENT(40, 40, 104, 62, PdfDecor.Mode.ENTETE),
        /** Facture proforma : pied de page ancré en bas, sans pagination. */
        FACTURE(PdfTheme.MARGE_DOCUMENT, PdfTheme.MARGE_DOCUMENT, 26,
                PdfTheme.HAUTEUR_PIED_FACTURE, PdfDecor.Mode.FACTURE),
        /** Fiche de création : aucun décor. */
        FICHE(PdfTheme.MARGE_DOCUMENT, PdfTheme.MARGE_DOCUMENT, 26, 40, PdfDecor.Mode.SANS_PIED);

        private final float margeGauche;
        private final float margeDroite;
        private final float margeHaut;
        private final float margeBas;
        private final PdfDecor.Mode decor;

        MiseEnPage(float margeGauche, float margeDroite, float margeHaut, float margeBas,
                   PdfDecor.Mode decor) {
            this.margeGauche = margeGauche;
            this.margeDroite = margeDroite;
            this.margeHaut = margeHaut;
            this.margeBas = margeBas;
            this.decor = decor;
        }
    }

    @FunctionalInterface
    interface Contenu {
        void ecrire(Document doc) throws Exception;
    }

    private PdfDocument() {
    }

    static byte[] generer(MiseEnPage miseEnPage, String libelleDocument, String titreMeta,
                          Contenu contenu, String messageErreur) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, miseEnPage.margeGauche, miseEnPage.margeDroite,
                miseEnPage.margeHaut, miseEnPage.margeBas);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PdfDecor(miseEnPage.decor, libelleDocument));
            doc.addTitle(titreMeta);
            doc.addAuthor(PdfTheme.NOM_CABINET);
            doc.addCreator(PdfTheme.NOM_CABINET);
            doc.open();
            contenu.ecrire(doc);
            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException(messageErreur, e);
        }
        return out.toByteArray();
    }

    static int nombrePages(byte[] pdf) {
        PdfReader lecteur = null;
        try {
            lecteur = new PdfReader(pdf);
            return lecteur.getNumberOfPages();
        } catch (IOException e) {
            return 1;
        } finally {
            if (lecteur != null) {
                lecteur.close();
            }
        }
    }
}
