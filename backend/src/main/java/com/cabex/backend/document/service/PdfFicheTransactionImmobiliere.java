package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Fiche de transaction immobilière, reprise de la fiche papier du cabinet
 * (actions, objet, achat/vente, régularisation, suivi et signatures) dans la
 * charte graphique du logo.
 */
final class PdfFicheTransactionImmobiliere {

    private static final String[] DOCUMENTS = {
            "PV de reconnaissance",
            "N° demande d'acquisition",
            "Acte de vente privée ou communale",
            "Situation juridique",
            "Acte de notoriété"
    };

    private PdfFicheTransactionImmobiliere() {
    }

    static byte[] generer(DemandeService demande, DossierTransactionImmobiliere dossier) {
        byte[] pdf = null;
        for (float echelle : PdfStyleFacture.ECHELLES) {
            pdf = construire(demande, dossier, new PdfStyleFacture(echelle));
            if (PdfDocument.nombrePages(pdf) <= 1) {
                break;
            }
        }
        return pdf;
    }

    private static byte[] construire(DemandeService demande,
                                     DossierTransactionImmobiliere dossier,
                                     PdfStyleFacture st) {
        String titreMeta = "Fiche transaction immobilière "
                + PdfFormats.valeur(demande.getReferenceDemande());
        return PdfDocument.generer(PdfDocument.MiseEnPage.FICHE,
                "Fiche transaction immobilière", titreMeta,
                doc -> ecrire(doc, demande, dossier, st),
                "Erreur de génération de la fiche de transaction immobilière");
    }

    private static void ecrire(Document doc, DemandeService demande,
                               DossierTransactionImmobiliere dossier, PdfStyleFacture st) {
        entete(doc, demande, st);
        demandeur(doc, demande, dossier, st);
        actions(doc, dossier, st);
        objet(doc, dossier, st);
        achatVente(doc, dossier, st);
        regularisation(doc, dossier, st);
        observation(doc, dossier, st);
        suivi(doc, dossier, st);
        signatures(doc, st);
    }

    // ------------------------------------------------------------------
    // En-tête et demandeur
    // ------------------------------------------------------------------

    private static void entete(Document doc, DemandeService demande, PdfStyleFacture st) {
        PdfComposants.enteteDocument(doc, st, "TRANSACTION", "IMMOBILIÈRE");

        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(st.ecart * 0.8f);
        meta.addCell(PdfComposants.faMeta(st, "N° DEMANDE",
                PdfFormats.valeur(demande.getReferenceDemande()), PdfTheme.ROUGE));
        meta.addCell(PdfComposants.faMeta(st, "DATE",
                PdfFormats.valeurDate(demande.getDateDemande()), PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "COMMERCIAL", commercial(demande),
                PdfTheme.PRIMAIRE));
        doc.add(meta);
    }

    private static String commercial(DemandeService demande) {
        if (demande.getCommercial() == null) {
            return "-";
        }
        String nom = PdfFormats.valeur(demande.getCommercial().getNomCommercial());
        String immatricule = demande.getCommercial().getImmatricule();
        return immatricule != null && !immatricule.isBlank()
                ? nom + "  ·  N° IM " + immatricule
                : nom;
    }

    private static void demandeur(Document doc, DemandeService demande,
                                  DossierTransactionImmobiliere dossier, PdfStyleFacture st) {
        Societe societe = demande.getSociete();
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "Demandeur"));
        b.addElement(ficheLigne(st, "Nom ou raison sociale du demandeur",
                PdfFormats.valeur(societe != null ? societe.getNomSociete() : null)));
        b.addElement(ficheLigne(st, "Nom du propriétaire précédent",
                PdfFormats.valeur(dossier.getNomProprietairePrecedent())));
        b.addElement(ficheLigne(st, "Contact / Mail / fb", contact(societe)));
        b.addElement(ficheLigne(st, "Adresse",
                PdfFormats.valeur(societe != null ? societe.getAdresse() : null)));
        doc.add(PdfComposants.boiteFiche(b));
    }

    private static String contact(Societe societe) {
        if (societe == null) {
            return "-";
        }
        StringBuilder texte = new StringBuilder();
        ajouter(texte, societe.getContact());
        ajouter(texte, societe.getEmail());
        ajouter(texte, societe.getFb());
        return texte.isEmpty() ? "-" : texte.toString();
    }

    private static void ajouter(StringBuilder texte, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return;
        }
        if (!texte.isEmpty()) {
            texte.append("  /  ");
        }
        texte.append(valeur);
    }

    // ------------------------------------------------------------------
    // Action et objet
    // ------------------------------------------------------------------

    private static void actions(Document doc, DossierTransactionImmobiliere dossier,
                                PdfStyleFacture st) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "1. Action"));
        b.addElement(choix(st, 3, new String[]{
                coche(dossier.getActionAchat()) + "  Achat",
                coche(dossier.getActionVente()) + "  Vente",
                coche(dossier.getActionLocation()) + "  Location",
                coche(dossier.getActionRegularisationPapiers()) + "  Régularisation des papiers",
                coche(dossier.getActionGestionImmobiliere()) + "  Gestion immobilière",
                coche(dossier.getActionAutre()) + "  Autre : "
                        + PdfFormats.valeurTexte(dossier.getActionAutrePrecision())
        }));
        doc.add(PdfComposants.boiteFiche(b));
    }

    private static void objet(Document doc, DossierTransactionImmobiliere dossier,
                              PdfStyleFacture st) {
        String objet = PdfFormats.code(dossier.getObjet());
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "2. Objet"));
        b.addElement(choix(st, 3, new String[]{
                coche("TERRAIN".equals(objet)) + "  Terrain",
                coche("MAISON".equals(objet)) + "  Maison",
                coche("AUTRE".equals(objet)) + "  Autre : "
                        + PdfFormats.valeurTexte(dossier.getObjetAutrePrecision())
        }));
        doc.add(PdfComposants.boiteFiche(b));
    }

    // ------------------------------------------------------------------
    // Achat / vente
    // ------------------------------------------------------------------

    private static void achatVente(Document doc, DossierTransactionImmobiliere dossier,
                                   PdfStyleFacture st) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});
        table.setSpacingBefore(8);
        table.setKeepTogether(true);

        table.addCell(enteteBloc(st, "ACHAT"));
        table.addCell(enteteBloc(st, "VENTE"));
        table.addCell(blocAchat(st, dossier));
        table.addCell(blocVente(st, dossier));
        doc.add(table);
    }

    private static PdfPCell blocAchat(PdfStyleFacture st, DossierTransactionImmobiliere d) {
        PdfPCell c = celluleBloc(st);
        c.addElement(ligneSimple(st, "Mesure", mesure(d.getAchatMesureM2())));
        c.addElement(ligneSimple(st, "Position", PdfFormats.valeur(d.getAchatPosition())));
        c.addElement(ligneSimple(st, "Axe ou emplacement",
                PdfFormats.valeur(d.getAchatAxeEmplacement())));
        c.addElement(ligneSimple(st, "Montant",
                d.getAchatMontant() != null ? PdfFormats.formatArgent(d.getAchatMontant()) : "—"));
        c.addElement(ouiNon(st, "Si maison : permis de construire", d.getAchatPermisConstruire()));
        return c;
    }

    private static PdfPCell blocVente(PdfStyleFacture st, DossierTransactionImmobiliere d) {
        PdfPCell c = celluleBloc(st);
        c.addElement(ligneSimple(st, "Mesure", mesure(d.getVenteMesureM2())));
        c.addElement(ligneSimple(st, "Position", PdfFormats.valeur(d.getVentePosition())));
        c.addElement(ligneSimple(st, "Axe ou emplacement",
                PdfFormats.valeur(d.getVenteAxeEmplacement())));
        c.addElement(ouiNon(st, "Prise de photo", d.getVentePrisePhoto()));
        c.addElement(ouiNon(st, "Visite", d.getVenteVisite()));
        c.addElement(ligneSimple(st, "Montant",
                d.getVenteMontant() != null ? PdfFormats.formatArgent(d.getVenteMontant()) : "—"));
        c.addElement(ouiNon(st, "Si maison : permis de construire", d.getVentePermisConstruire()));
        return c;
    }

    private static PdfPCell enteteBloc(PdfStyleFacture st, String titre) {
        PdfPCell c = new PdfPCell(new Phrase(titre, st.th));
        c.setBackgroundColor(PdfTheme.PRIMAIRE);
        c.setBorderColor(PdfTheme.PRIMAIRE);
        c.setBorderWidth(0.5f);
        c.setPadding(st.padV);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private static PdfPCell celluleBloc(PdfStyleFacture st) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.5f);
        c.setPadding(st.padCarte);
        return c;
    }

    private static Paragraph ligneSimple(PdfStyleFacture st, String libelle, String valeur) {
        Paragraph p = new Paragraph();
        p.setLeading(st.td.getSize() * 1.45f);
        p.setSpacingBefore(2f);
        p.add(new Chunk(libelle + " : ", st.petitGras));
        p.add(new Chunk(valeur == null || valeur.isEmpty() ? "—" : valeur, st.td));
        return p;
    }

    private static Paragraph ouiNon(PdfStyleFacture st, String libelle, Boolean valeur) {
        boolean oui = Boolean.TRUE.equals(valeur);
        boolean non = Boolean.FALSE.equals(valeur);
        Paragraph p = new Paragraph();
        p.setLeading(st.td.getSize() * 1.45f);
        p.setSpacingBefore(2f);
        p.add(new Chunk(libelle + " : ", st.petitGras));
        p.add(new Chunk(coche(oui) + "  OUI      " + coche(non) + "  NON", st.td));
        return p;
    }

    private static String mesure(java.math.BigDecimal valeur) {
        return valeur == null ? "—" : PdfFormats.formatMontant(valeur) + " m²";
    }

    // ------------------------------------------------------------------
    // Régularisation, observation, suivi
    // ------------------------------------------------------------------

    private static void regularisation(Document doc, DossierTransactionImmobiliere dossier,
                                       PdfStyleFacture st) {
        String type = PdfFormats.code(dossier.getRegularisationTypeTerrain());
        String situation = PdfFormats.code(dossier.getRegularisationSituationTerrain());

        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "Régularisation de papier"));
        b.addElement(libelleChoix(st, "3/ Type de terrain :"));
        b.addElement(choix(st, 3, new String[]{
                coche("COMMUNAL_DOMAINE".equals(type)) + "  Communal / Domanial",
                coche("PRIVE".equals(type)) + "  Privé",
                coche("HERITAGE_DON".equals(type)) + "  Héritage ou Don"
        }));
        b.addElement(libelleChoix(st, "4/ Situation du terrain :"));
        b.addElement(choix(st, 2, new String[]{
                coche("TITRE_BORNE".equals(situation)) + "  Titré borné",
                coche("RECONNAISSANCE".equals(situation)) + "  Reconnaissance"
        }));
        b.addElement(libelleChoix(st, "5/ Liste des documents existants :"));
        b.addElement(choix(st, 3, new String[]{
                coche(Boolean.TRUE.equals(dossier.getDocumentPvReconnaissance())) + "  " + DOCUMENTS[0],
                coche(Boolean.TRUE.equals(dossier.getDocumentNumeroDemandeAcquisition())) + "  " + DOCUMENTS[1],
                coche(Boolean.TRUE.equals(dossier.getDocumentActeVentePriveeCommunale())) + "  " + DOCUMENTS[2],
                coche(Boolean.TRUE.equals(dossier.getDocumentSituationJuridique())) + "  " + DOCUMENTS[3],
                coche(Boolean.TRUE.equals(dossier.getDocumentActeNotoriete())) + "  " + DOCUMENTS[4]
        }));
        doc.add(PdfComposants.boiteFiche(b));
    }

    private static void observation(Document doc, DossierTransactionImmobiliere dossier,
                                    PdfStyleFacture st) {
        PdfPCell b = ficheBoite(st);
        b.addElement(ficheTitre(st, "Observation"));
        Paragraph p = new Paragraph(PdfFormats.valeurTexte(dossier.getObservations()), st.td);
        p.setLeading(st.td.getSize() * 1.45f);
        p.setSpacingBefore(4);
        b.addElement(p);
        b.setMinimumHeight(st.hObservations);
        doc.add(PdfComposants.boiteFiche(b));
    }

    private static void suivi(Document doc, DossierTransactionImmobiliere dossier,
                              PdfStyleFacture st) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.35f, 1f});
        t.setSpacingBefore(8);
        t.setKeepTogether(true);

        PdfPCell gauche = celluleBloc(st);
        gauche.addElement(ligneSimple(st, "Nombre de commerciaux sur terrain voulus",
                PdfFormats.valeurNombre(dossier.getNombreCommerciauxTerrain())));
        gauche.addElement(ligneSimple(st, "Commission du CN",
                dossier.getCommissionCn() != null
                        ? PdfFormats.formatArgent(dossier.getCommissionCn()) : "—"));
        t.addCell(gauche);

        PdfPCell droite = celluleBloc(st);
        droite.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph abonnement = new Paragraph();
        abonnement.setLeading(st.td.getSize() * 1.45f);
        abonnement.add(new Chunk(coche(Boolean.TRUE.equals(dossier.getPrestationAbonnementMensuel())),
                st.td));
        abonnement.add(new Chunk("  Prestation abonnement mensuel", st.td));
        droite.addElement(abonnement);
        t.addCell(droite);
        doc.add(t);
    }

    private static void signatures(Document doc, PdfStyleFacture st) {
        PdfPTable signatures = PdfComposants.signaturesStyle(st, st.hSignature,
                new String[]{"Le Client"},
                new String[]{"Le Commercial"},
                new String[]{"Le Responsable du Dossier"},
                new String[]{"Manager"});
        signatures.setSpacingBefore(st.ecart);
        doc.add(signatures);
    }

    // ------------------------------------------------------------------
    // Petits composants de la fiche
    // ------------------------------------------------------------------

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

    private static Paragraph ficheLigne(PdfStyleFacture st, String libelle, String valeur) {
        Paragraph p = new Paragraph();
        p.setLeading(st.td.getSize() * 1.5f);
        p.setSpacingBefore(2f);
        p.add(new Chunk(libelle + " : ", st.petitGras));
        p.add(new Chunk(valeur == null || valeur.isEmpty() ? "—" : valeur, st.td));
        return p;
    }

    private static Paragraph libelleChoix(PdfStyleFacture st, String texte) {
        Paragraph p = new Paragraph(texte, st.petitGras);
        p.setSpacingBefore(4);
        p.setSpacingAfter(1);
        return p;
    }

    private static PdfPTable choix(PdfStyleFacture st, int colonnes, String[] cellules) {
        PdfPTable table = new PdfPTable(colonnes);
        table.setWidthPercentage(100);
        table.setSpacingBefore(3);
        for (String texte : cellules) {
            PdfPCell c = new PdfPCell(new Phrase(texte, st.td));
            c.setBorder(Rectangle.NO_BORDER);
            c.setPaddingTop(1);
            c.setPaddingBottom(2);
            c.setPaddingLeft(0);
            c.setPaddingRight(4);
            table.addCell(c);
        }
        int reste = cellules.length % colonnes;
        if (reste > 0) {
            for (int i = reste; i < colonnes; i++) {
                PdfPCell vide = new PdfPCell(new Phrase(" ", st.td));
                vide.setBorder(Rectangle.NO_BORDER);
                table.addCell(vide);
            }
        }
        return table;
    }

    private static String coche(boolean coche) {
        return coche ? "[X]" : "[ ]";
    }
}
