package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.dossier.entity.DossierAssocie;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Fiche de création d'entreprise ou de société (reprend la fiche papier,
 * design aligné sur la facture proforma).
 */
final class PdfFicheCreation {

    private PdfFicheCreation() {
    }

    static byte[] generer(DemandeService demande, DossierCreation dossier) {
        String titreMeta = "Fiche de création " + PdfFormats.valeur(demande.getReferenceDemande());
        return PdfDocument.generer(PdfDocument.MiseEnPage.FICHE, "Fiche de création",
                titreMeta, doc -> ecrire(doc, demande, dossier),
                "Erreur de génération de la fiche de création");
    }

    private static void ecrire(Document doc, DemandeService demande, DossierCreation dossier) {
        PdfStyleFacture st = new PdfStyleFacture(1f);
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
        b.addElement(ficheLigne(st, "Nombre d'associés",
                PdfFormats.valeurNombre(dossier.getNombreAssociesDeclare())));
        if (dossier.getAssocies() != null && !dossier.getAssocies().isEmpty()) {
            b.addElement(tableauAssocies(st, dossier));
        }
        b.addElement(ficheLigne(st, "Nom du gérant",
                PdfFormats.valeurTexte(dossier.getNomGerant())));
        b.addElement(ficheLigne(st, "Fonction",
                PdfFormats.valeurTexte(dossier.getFonctionGerant())));
        b.addElement(ficheLigne(st, "",
                ficheCase(Boolean.TRUE.equals(dossier.getLocationGerance()))
                        + "  Location de gérance"));
        return b;
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

    private static PdfPCell adresseSiege(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "4. ADRESSE DU SIÈGE ET DE L'ÉTABLISSEMENT"));
        b.addElement(ficheLigne(st, "Adresse", PdfFormats.valeurTexte(dossier.getAdresseSiege())));
        b.addElement(ficheLigne(st, "Ville", PdfFormats.valeurTexte(dossier.getVille())));
        b.addElement(ficheLigne(st, "Commune", PdfFormats.valeurTexte(dossier.getCommune())));
        b.addElement(ficheLigne(st, "District", PdfFormats.valeurTexte(dossier.getDistrict())));
        b.addElement(ficheLigne(st, "Région", PdfFormats.valeurTexte(dossier.getRegion())));

        String hebergement = PdfFormats.code(dossier.getTypeHebergement());
        b.addElement(ficheLigne(st, "",
                ficheCase(PdfFormats.estValeur(hebergement, "DOMICILE_GERANT"))
                        + "  Au domicile du gérant ou du propriétaire"
                        + "      " + ficheCase(PdfFormats.estValeur(hebergement, "LOCATION_CABINET"))
                        + "  Location de siège au Cabinet Nakay"));
        b.addElement(ficheLigne(st, "Autres", PdfFormats.estValeur(hebergement, "AUTRE")
                ? PdfFormats.valeurTexte(dossier.getHebergementPrecision()) : ""));
        b.addElement(ficheLigne(st, "", contratBail(dossier)));
        return b;
    }

    private static String contratBail(DossierCreation dossier) {
        StringBuilder texte = new StringBuilder();
        texte.append(ficheCase(dossier.getContratBail() != null))
                .append("  Possédez-vous un contrat de bail ?");
        if (dossier.getContratBail() != null) {
            texte.append(Boolean.TRUE.equals(dossier.getContratBail()) ? "  Oui" : "  Non");
        }
        if (dossier.getContratBailDetail() != null && !dossier.getContratBailDetail().isBlank()) {
            texte.append(" — ").append(dossier.getContratBailDetail());
        }
        return texte.toString();
    }

    private static PdfPCell objetSocial(PdfStyleFacture st, DossierCreation dossier) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "5. PRINCIPALES ACTIVITÉS DE L'OBJET SOCIAL"));
        b.addElement(ficheLigne(st, "", PdfFormats.valeurTexte(dossier.getObjetSocial())));
        b.addElement(ficheTitre(st, "DATE SOUHAITÉE POUR LE DÉBUT DE L'ACTIVITÉ"));
        b.addElement(ficheLigne(st, "Date",
                PdfFormats.valeurDate(dossier.getDateSouhaiteeDebut())));

        String regime = PdfFormats.code(dossier.getRegimeActivite());
        b.addElement(ficheLigne(st, "",
                "Permanente " + ficheCase(PdfFormats.estValeur(regime, "PERMANENTE"))
                        + "      Saisonnière " + ficheCase(PdfFormats.estValeur(regime, "SAISONNIERE"))
                        + "      Ambulant " + ficheCase(PdfFormats.estValeur(regime, "AMBULANT"))));
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

    /** Case cochée ([X]) ou vide ([ ]) façon fiche papier. */
    private static String ficheCase(boolean coche) {
        return coche ? "[X]" : "[ ]";
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

    private static Paragraph ficheCases(PdfStyleFacture st, String[][] options, String selection) {
        Paragraph p = new Paragraph();
        p.setLeading(st.td.getSize() * 1.6f);
        p.setSpacingBefore(2);
        for (int i = 0; i < options.length; i++) {
            if (i > 0) {
                p.add(new Chunk("      ", st.td));
            }
            p.add(new Chunk(ficheCase(PdfFormats.estValeur(selection, options[i][0]))
                    + "  " + options[i][1], st.td));
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
