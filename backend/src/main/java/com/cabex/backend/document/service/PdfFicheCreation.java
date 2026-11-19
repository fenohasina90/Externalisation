package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.dossier.entity.DossierAssocie;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;

/**
 * Fiche de création d'entreprise ou de société (reprend la fiche papier,
 * design aligné sur la facture proforma).
 */
final class PdfFicheCreation {

    private PdfFicheCreation() {
    }

    static byte[] generer(DemandeService demande, DossierCreation dossier) {
        byte[] pdf = null;
        for (float echelle : PdfStyleFacture.ECHELLES) {
            PdfStyleFacture st = new PdfStyleFacture(echelle);
            String titreMeta = "Fiche de création " + PdfFormats.valeur(demande.getReferenceDemande());
            pdf = PdfDocument.generer(PdfDocument.MiseEnPage.FICHE, "Fiche de création",
                    titreMeta, doc -> ecrire(doc, demande, dossier, st),
                    "Erreur de génération de la fiche de création");
            if (PdfDocument.nombrePages(pdf) <= 1) {
                break;
            }
        }
        return pdf;
    }

    private static void ecrire(Document doc, DemandeService demande, DossierCreation dossier,
                               PdfStyleFacture st) {
        entete(doc, st);
        bandeauReferences(doc, demande, st);

        doc.add(PdfComposants.boiteFiche(formeJuridique(st, dossier)));
        doc.add(PdfComposants.boiteFiche(societe(st, dossier)));
        doc.add(PdfComposants.boiteFiche(secteurActivite(st, dossier)));
        doc.add(PdfComposants.boiteFiche(adresseSiege(st, dossier)));
        doc.add(PdfComposants.boiteFiche(objetSocial(st, dossier)));
        doc.add(PdfComposants.boiteFiche(observations(st, dossier)));

        doc.add(signatures(st));
    }

    // ------------------------------------------------------------------
    // En-tête
    // ------------------------------------------------------------------

    private static void entete(Document doc, PdfStyleFacture st) {
        PdfComposants.enteteDocument(doc, st, "FICHE DE CRÉATION", "ENTREPRISE OU SOCIÉTÉ");
    }

    private static void bandeauReferences(Document doc, DemandeService demande, PdfStyleFacture st) {
        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(st.ecart * 0.8f);
        meta.addCell(PdfComposants.faMeta(st, "DEMANDE",
                PdfFormats.valeur(demande.getReferenceDemande()), PdfTheme.ROUGE));
        meta.addCell(PdfComposants.faMeta(st, "DATE",
                PdfFormats.valeurDate(demande.getDateDemande()), PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "COMMERCIAL", commercial(demande), PdfTheme.PRIMAIRE));
        doc.add(meta);
    }

    private static String commercial(DemandeService demande) {
        if (demande.getCommercial() == null) {
            return "-";
        }
        return demande.getCommercial().getNomCommercial()
                + " — N° IM : " + PdfFormats.valeur(demande.getCommercial().getImmatricule());
    }

    // ------------------------------------------------------------------
    // Rubriques
    // ------------------------------------------------------------------

    private static PdfPCell formeJuridique(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "1. FORME JURIDIQUE"));
        b.addElement(ficheCases(st, new String[][]{
                {"INDIVIDUELLE", "Individuelle"},
                {"SARL", "SARL"},
                {"SARLU", "SARLU"},
                {"SA", "SA"},
                {"AUTRE", "Autre à préciser : "
                        + PdfFormats.valeurTexte(dossier.getFormeJuridiquePrecision())}
        }, PdfFormats.code(dossier.getFormeJuridique())));
        b.addElement(ficheLigne(st, "Dénomination",
                PdfFormats.valeurTexte(dossier.getDenomination())));
        b.addElement(ficheLigne(st, "Siège social actuel",
                PdfFormats.valeurTexte(dossier.getSiegeSocialActuel())));
        return b;
    }

    private static PdfPCell societe(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "2. SI SOCIÉTÉ"));
        b.addElement(ficheLigne(st, "Capital social",
                PdfFormats.formatMontant(dossier.getCapitalSocial()) + " Ariary"));
        b.addElement(ficheLigne(st, "Nombre d'associés", nombreAssocies(dossier)));
        if (dossier.getAssocies() != null && !dossier.getAssocies().isEmpty()) {
            b.addElement(tableauAssocies(st, dossier));
        }
        b.addElement(ficheSousTitre(st, "GÉRANCE"));
        b.addElement(ficheLigne(st, "Nom du gérant",
                PdfFormats.valeurTexte(dossier.getNomGerant())));
        b.addElement(ficheLigne(st, "Fonction",
                PdfFormats.valeurTexte(dossier.getFonctionGerant())));
        b.addElement(ficheLigneCase(st,
                Boolean.TRUE.equals(dossier.getLocationGerance()), "Location de gérance"));
        return b;
    }

    /**
     * Nombre réel d'associés : lignes d'associé réellement renseignées ; à défaut,
     * la déclaration saisie ; « — » sinon (ex. entreprise individuelle).
     */
    private static String nombreAssocies(DossierCreation dossier) {
        if (dossier.getAssocies() != null) {
            long nb = dossier.getAssocies().stream()
                    .filter(a -> a.getNom() != null && !a.getNom().isBlank())
                    .count();
            if (nb > 0) {
                return String.valueOf(nb);
            }
        }
        Integer declare = dossier.getNombreAssociesDeclare();
        if (declare != null && declare > 0) {
            return String.valueOf(declare);
        }
        return "—";
    }

    private static PdfPTable tableauAssocies(PdfStyleFacture st, DossierCreation dossier) {
        PdfPTable associes = new PdfPTable(4);
        associes.setWidthPercentage(100);
        associes.setSpacingBefore(4);
        ficheMiniTh(associes, st, "NOM DES ASSOCIÉS");
        ficheMiniTh(associes, st, "PART SOCIALE");
        ficheMiniTh(associes, st, "ADRESSE MAIL");
        ficheMiniTh(associes, st, "CONTACT");
        boolean zebra = false;
        for (DossierAssocie associe : dossier.getAssocies()) {
            ficheMiniTd(associes, st, PdfFormats.valeurTexte(associe.getNom()), zebra);
            ficheMiniTd(associes, st, PdfFormats.valeurTexte(associe.getPartSociale()), zebra);
            ficheMiniTd(associes, st, PdfFormats.valeurTexte(associe.getEmail()), zebra);
            ficheMiniTd(associes, st, PdfFormats.valeurTexte(associe.getContact()), zebra);
            zebra = !zebra;
        }
        return associes;
    }

    private static PdfPCell secteurActivite(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "3. SECTEUR D'ACTIVITÉ"));
        b.addElement(ficheLigne(st, "Secteur",
                PdfFormats.valeurTexte(dossier.getSecteurActivite())));
        b.addElement(ficheLigne(st, "Autorisation spéciale",
                PdfFormats.valeurTexte(dossier.getAutorisationSpeciale())));
        return b;
    }

    /** Adresse sur une grille 2×3 compacte (libellé | valeur | libellé | valeur). */
    private static PdfPTable grilleAdresse(PdfStyleFacture st, DossierCreation dossier) {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 2.3f, 1f, 2.3f});
        t.setSpacingBefore(4);
        t.setKeepTogether(true);
        celluleAdresse(t, st, "Adresse", PdfFormats.valeurTexte(dossier.getAdresseSiege()));
        celluleAdresse(t, st, "Ville", PdfFormats.valeurTexte(dossier.getVille()));
        celluleAdresse(t, st, "Commune", PdfFormats.valeurTexte(dossier.getCommune()));
        celluleAdresse(t, st, "", "");
        celluleAdresse(t, st, "District", PdfFormats.valeurTexte(dossier.getDistrict()));
        celluleAdresse(t, st, "Région", PdfFormats.valeurTexte(dossier.getRegion()));
        return t;
    }

    private static void celluleAdresse(PdfPTable t, PdfStyleFacture st,
                                       String libelle, String valeur) {
        PdfPCell c = new PdfPCell(new Paragraph(
                (libelle.isEmpty() ? "" : libelle + " : "), st.petitGras));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(2f);
        c.setPaddingBottom(2f);
        c.setPaddingLeft(0);
        c.setPaddingRight(0);
        if (libelle.isEmpty()) {
            c.setPadding(0);
        }
        t.addCell(c);
        PdfPCell v = new PdfPCell(new Paragraph(
                valeur == null || valeur.isEmpty() ? "—" : valeur, st.td));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPaddingTop(2f);
        v.setPaddingBottom(2f);
        v.setPaddingLeft(0);
        v.setPaddingRight(6f);
        t.addCell(v);
    }

    private static PdfPCell adresseSiege(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "4. ADRESSE DU SIÈGE ET DE L'ÉTABLISSEMENT"));
        b.addElement(grilleAdresse(st, dossier));

        b.addElement(ficheSousTitre(st, "HÉBERGEMENT DU SIÈGE"));
        String hebergement = PdfFormats.code(dossier.getTypeHebergement());
        Paragraph hebergementP = new Paragraph();
        hebergementP.setSpacingBefore(3);
        hebergementP.setSpacingAfter(2);
        hebergementP.setLeading(st.td.getSize() * 1.7f);
        hebergementP.add(caseChunk(PdfFormats.estValeur(hebergement, "DOMICILE_GERANT")));
        hebergementP.add(new Chunk("  Au domicile du gérant ou du propriétaire", st.td));
        hebergementP.add(new Chunk("        ", st.td));
        hebergementP.add(caseChunk(PdfFormats.estValeur(hebergement, "LOCATION_CABINET")));
        hebergementP.add(new Chunk("  Location de siège au Cabinet Nakay", st.td));
        b.addElement(hebergementP);
        if (PdfFormats.estValeur(hebergement, "AUTRE")) {
            b.addElement(ficheLigne(st, "Précision (autres)",
                    PdfFormats.valeurTexte(dossier.getHebergementPrecision())));
        }
        b.addElement(contratBailLigne(st, dossier));
        return b;
    }

    private static Paragraph contratBailLigne(PdfStyleFacture st, DossierCreation dossier) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3);
        p.setSpacingAfter(2);
        p.setLeading(st.td.getSize() * 1.6f);
        p.add(caseChunk(dossier.getContratBail() != null));
        p.add(new Chunk("  Possédez-vous un contrat de bail ?", st.td));
        if (dossier.getContratBail() != null) {
            p.add(new Chunk(Boolean.TRUE.equals(dossier.getContratBail()) ? "  Oui" : "  Non",
                    st.tdGras));
        }
        if (dossier.getContratBailDetail() != null && !dossier.getContratBailDetail().isBlank()) {
            p.add(new Chunk(" — " + dossier.getContratBailDetail(), st.td));
        }
        return p;
    }

    private static PdfPCell objetSocial(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "5. PRINCIPALES ACTIVITÉS DE L'OBJET SOCIAL"));
        b.addElement(ficheLigne(st, "Objet social", PdfFormats.valeurTexte(dossier.getObjetSocial())));
        b.addElement(ficheSousTitre(st, "DATE SOUHAITÉE POUR LE DÉBUT DE L'ACTIVITÉ"));
        b.addElement(ficheLigne(st, "Date",
                PdfFormats.valeurDate(dossier.getDateSouhaiteeDebut())));

        String regime = PdfFormats.code(dossier.getRegimeActivite());
        Paragraph regimeP = new Paragraph();
        regimeP.setSpacingBefore(4);
        regimeP.setSpacingAfter(2);
        regimeP.setLeading(st.td.getSize() * 1.8f);
        regimeP.add(caseChunk(PdfFormats.estValeur(regime, "PERMANENTE")));
        regimeP.add(new Chunk("  Permanente", st.td));
        regimeP.add(new Chunk("        ", st.td));
        regimeP.add(caseChunk(PdfFormats.estValeur(regime, "SAISONNIERE")));
        regimeP.add(new Chunk("  Saisonnière", st.td));
        regimeP.add(new Chunk("        ", st.td));
        regimeP.add(caseChunk(PdfFormats.estValeur(regime, "AMBULANT")));
        regimeP.add(new Chunk("  Ambulant", st.td));
        b.addElement(regimeP);
        b.addElement(ficheLigne(st,
                "Activités exercées dans l'établissement ou succursale ou filiale",
                PdfFormats.valeurTexte(dossier.getActivitesEtablissement())));
        return b;
    }

    private static PdfPCell observations(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "6. OBSERVATIONS"));
        b.addElement(ficheLigne(st, "", PdfFormats.valeurTexte(dossier.getObservations())));
        b.addElement(ficheLigne(st, "Assujettissement à la TVA",
                PdfFormats.valeurTexte(dossier.getAssujettissementTva())));
        b.addElement(ficheLigne(st, "Avez-vous d'autre société ?",
                PdfFormats.valeurTexte(dossier.getAutreSociete())));
        b.addElement(ficheLigne(st, "Situation matrimoniale",
                PdfFormats.valeurTexte(dossier.getSituationMatrimoniale())));
        b.addElement(ficheLigne(st, "Régime d'imposition",
                PdfFormats.valeurTexte(dossier.getRegimeImposition())));
        return b;
    }

    private static PdfPTable signatures(PdfStyleFacture st) {
        PdfPTable signatures = PdfComposants.signaturesStyle(st, st.hSignature,
                new String[]{"Le Client"},
                new String[]{"Le Commercial"},
                new String[]{"Le Responsable du dossier"},
                new String[]{"Manager"});
        signatures.setSpacingBefore(st.ecart);
        return signatures;
    }

    // ------------------------------------------------------------------
    // Éléments de la fiche
    // ------------------------------------------------------------------

    private static final Font F_CASE_COCHE =
            new Font(Font.ZAPFDINGBATS, 10f, Font.NORMAL, PdfTheme.VERT_TEXTE);
    private static final Font F_CASE_VIDE =
            new Font(Font.ZAPFDINGBATS, 10f, Font.NORMAL, new Color(190, 205, 185));

    /** Filet horizontal discret séparant deux groupes d'une même rubrique. */
    private static PdfPTable ficheSeparateur() {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);
        t.setSpacingAfter(6);
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.6f);
        t.addCell(c);
        return t;
    }

    /** Sous-titre discret (style libelleChoix) pour regrouper des lignes. */
    private static Paragraph ficheSousTitre(PdfStyleFacture st, String texte) {
        Paragraph p = new Paragraph(texte, st.petitGras);
        p.setSpacingBefore(4);
        p.setSpacingAfter(3);
        return p;
    }

    /** Puce typographique : « ✔ » verte si cochée, « ❑ » gris clair sinon. */
    private static Chunk caseChunk(boolean coche) {
        return new Chunk(coche ? "4" : "q", coche ? F_CASE_COCHE : F_CASE_VIDE);
    }

    /** « Puce  Libellé » sur une ligne. */
    private static Paragraph ficheLigneCase(PdfStyleFacture st, boolean coche, String texte) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3);
        p.setSpacingAfter(2);
        p.setLeading(st.td.getSize() * 1.6f);
        p.add(caseChunk(coche));
        p.add(new Chunk("  " + texte, st.td));
        return p;
    }

    /** Rubrique de la fiche : encadré à liseré vert vif, fond blanc. */
    private static PdfPCell ficheBoite(PdfStyleFacture st) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setBorderWidthLeft(2.5f);
        c.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        c.setPadding(st.padCarte);
        c.setPaddingLeft(st.padCarte + 2f);
        return c;
    }

    private static Paragraph ficheTitre(PdfStyleFacture st, String texte) {
        Paragraph p = new Paragraph(PdfTheme.upper(texte), st.section);
        p.setSpacingAfter(3);
        return p;
    }

    /** « Libellé : valeur » — libellé en gras, valeur plus grande ; « — » si vide. */
    private static Paragraph ficheLigne(PdfStyleFacture st, String libelle, String valeur) {
        Paragraph p = new Paragraph();
        p.setLeading(st.td.getSize() * 1.5f);
        p.setSpacingBefore(2f);
        if (libelle != null && !libelle.isEmpty()) {
            p.add(new Chunk(libelle + " : ", st.petitGras));
        }
        p.add(new Chunk(valeur == null || valeur.isEmpty() ? "—" : valeur, st.td));
        return p;
    }

    /** Options cochables sur une ligne, disposition horizontale façon chips. */
    private static Paragraph ficheCases(PdfStyleFacture st, String[][] options, String selection) {
        Paragraph p = new Paragraph();
        p.setLeading(st.td.getSize() * 1.9f);
        p.setSpacingBefore(4);
        for (int i = 0; i < options.length; i++) {
            if (i > 0) {
                p.add(new Chunk("        ", st.td));
            }
            p.add(caseChunk(PdfFormats.estValeur(selection, options[i][0])));
            p.add(new Chunk("  " + options[i][1], st.td));
        }
        return p;
    }

    private static void ficheMiniTh(PdfPTable table, PdfStyleFacture st, String texte) {
        PdfPCell c = new PdfPCell(new Phrase(texte, st.th));
        c.setBackgroundColor(PdfTheme.PRIMAIRE);
        c.setPadding(4);
        table.addCell(c);
    }

    private static void ficheMiniTd(PdfPTable table, PdfStyleFacture st,
                                    String texte, boolean zebra) {
        PdfPCell c = new PdfPCell(new Phrase(texte.isEmpty() ? "—" : texte, st.td));
        c.setPadding(4);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        if (zebra) {
            c.setBackgroundColor(PdfTheme.ZEBRA);
        }
        table.addCell(c);
    }
}
