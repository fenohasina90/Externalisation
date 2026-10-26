package com.cabex.backend.document.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Blocs spécifiques à la facture proforma (conditions de règlement, pied de page groupe).
 */
final class PdfFactureComposants {

    private PdfFactureComposants() {
    }

    static PdfPTable conditionsReglement(PdfStyleFacture st) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1.45f});
        t.setSpacingBefore(st.ecart);
        t.setKeepTogether(true);

        t.addCell(blocCoordonneesBancaires(st));
        t.addCell(blocModesReglement(st));
        t.addCell(notePaiement(st));
        return t;
    }

    private static PdfPCell blocCoordonneesBancaires(PdfStyleFacture st) {
        PdfPCell banque = PdfComposants.faBoite(st);
        banque.addElement(new Paragraph("COORDONNÉES BANCAIRES", st.carteTitre));
        for (int i = 0; i < PdfTheme.COORDONNEES_BANCAIRES.length; i++) {
            Paragraph ligne = new Paragraph(PdfTheme.COORDONNEES_BANCAIRES[i],
                    i == 1 ? st.petitGras : st.petit);
            ligne.setLeading(st.petit.getSize() * 1.3f);
            ligne.setSpacingBefore(i == 0 ? st.ecart * 0.4f : 1.5f);
            banque.addElement(ligne);
        }
        return banque;
    }

    private static PdfPCell blocModesReglement(PdfStyleFacture st) {
        PdfPCell modes = PdfComposants.faBoite(st);
        modes.addElement(new Paragraph("MODE DE RÈGLEMENT", st.carteTitre));
        modes.addElement(mode(st, "Par chèque",
                "Banque ..................   N° chèque ............   Date ..........   Montant .........."));
        modes.addElement(mode(st, "Par virement",
                "Banque ..................   Date ..........   Montant .........."));
        modes.addElement(mode(st, "Par espèces", null));
        modes.addElement(mode(st, "Par mobile money", null));
        return modes;
    }

    private static Paragraph mode(PdfStyleFacture st, String titre, String detail) {
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

    private static PdfPCell notePaiement(PdfStyleFacture st) {
        PdfPCell note = new PdfPCell(new Phrase("À payer dès la réception de la facture", st.note));
        note.setColspan(2);
        note.setHorizontalAlignment(Element.ALIGN_CENTER);
        note.setBackgroundColor(PdfTheme.VERT_PALE);
        note.setBorderColor(PdfTheme.GRIS_BORDURE);
        note.setBorderWidth(0.5f);
        note.setPadding(st.padV + 1f);
        return note;
    }

    /** Pied de page de la facture : entités du groupe sur 4 colonnes puis contacts. */
    static PdfPTable piedFacture() {
        PdfPTable t = new PdfPTable(4);

        Phrase titre = new Phrase();
        titre.add(new Chunk("SOCIÉTÉ GROUPE EXCELLENT", PdfTheme.F_CARTE_TITRE));
        titre.add(new Chunk("     Capital : 222 126 000 Ar", PdfTheme.F_PETIT_ITALIQUE));
        PdfPCell entete = new PdfPCell(titre);
        entete.setColspan(4);
        entete.setBorder(Rectangle.TOP);
        entete.setBorderColor(PdfTheme.VERT_MARQUE);
        entete.setBorderWidth(1.5f);
        entete.setPaddingLeft(0);
        entete.setPaddingTop(5);
        entete.setPaddingBottom(3);
        t.addCell(entete);

        for (String entite : PdfTheme.GROUPE) {
            PdfPCell e = new PdfPCell(new Phrase("•  " + entite, PdfTheme.F_MENTION));
            e.setBorder(Rectangle.NO_BORDER);
            e.setPadding(1.5f);
            e.setPaddingLeft(0);
            t.addCell(e);
        }

        PdfPCell contacts = new PdfPCell(new Phrase(
                "Contact Tanà : " + PdfTheme.CONTACT_TANA
                        + "     Contact Majunga : " + PdfTheme.CONTACT_MAJUNGA
                        + "     Mail : " + PdfTheme.MAIL_NAKAY
                        + "     Facebook : " + PdfTheme.FACEBOOK, PdfTheme.F_MENTION));
        contacts.setColspan(4);
        contacts.setHorizontalAlignment(Element.ALIGN_CENTER);
        contacts.setBorder(Rectangle.TOP);
        contacts.setBorderColor(PdfTheme.GRIS_BORDURE);
        contacts.setBorderWidth(0.5f);
        contacts.setPaddingTop(5);
        t.addCell(contacts);
        return t;
    }
}
