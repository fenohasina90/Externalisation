package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.service.entity.DomaineService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;

/**
 * Fiche récapitulative d'une demande d'externalisation.
 */
final class PdfRecapitulatif {

    private PdfRecapitulatif() {
    }

    static byte[] generer(DemandeService demande, List<DomaineService> domaines) {
        List<DomaineService> domainesTries = domaines != null ? domaines : List.of();
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT, "Fiche récapitulative",
                "Récapitulatif de demande " + PdfFormats.valeur(demande.getReferenceDemande()),
                doc -> ecrire(doc, demande, domainesTries),
                "Erreur de génération du récapitulatif de demande");
    }

    private static void ecrire(Document doc, DemandeService demande, List<DomaineService> domaines) {
        PdfStyleFacture st = new PdfStyleFacture(1f);
        entete(doc);
        bandeauReferences(doc, demande, st);
        cartesSociete(doc, demande, st);
        profilChiffre(doc, demande.getSociete(), st);
        domaine(doc, demande, domaines);
        services(doc, demande, st);
        delai(doc, st);
        observation(doc, demande, st);
        signatures(doc, st);
    }

    /** Lignes dans l'ordre d'insertion (id croissant). */
    private static List<LigneService> lignesTriees(DemandeService demande) {
        if (demande.getLignes() == null) {
            return List.of();
        }
        List<LigneService> lignes = new ArrayList<>(demande.getLignes());
        lignes.sort(java.util.Comparator.comparing(LigneService::getId,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        return lignes;
    }

    /** Identifiants des domaines des services demandés (cocher plusieurs cases si besoin). */
    private static Set<Integer> domainesDemandes(DemandeService demande) {
        Set<Integer> ids = new HashSet<>();
        for (LigneService ligne : lignesTriees(demande)) {
            if (ligne.getService() != null && ligne.getService().getDomaine() != null
                    && ligne.getService().getDomaine().getId() != null) {
                ids.add(ligne.getService().getDomaine().getId());
            }
        }
        return ids;
    }

    private static void entete(Document doc) {
        PdfPTable titre = new PdfPTable(2);
        titre.setWidthPercentage(100);
        titre.setWidths(new float[]{3.2f, 2f});

        PdfPCell gauche = new PdfPCell();
        gauche.setBorder(Rectangle.NO_BORDER);
        gauche.setPadding(0);
        gauche.addElement(new Paragraph("FICHE RÉCAPITULATIVE", PdfTheme.F_TITRE));
        Paragraph sous = new Paragraph("Demande d'externalisation de services", PdfTheme.F_SOUS_TITRE);
        sous.setSpacingBefore(2);
        gauche.addElement(sous);
        titre.addCell(gauche);

        PdfPCell badge = new PdfPCell(new Phrase("EXTERNALISATION",
                PdfTheme.font(13, Font.BOLD, Color.WHITE)));
        badge.setBackgroundColor(PdfTheme.PRIMAIRE);
        badge.setBorder(Rectangle.BOTTOM);
        badge.setBorderColor(PdfTheme.ROUGE);
        badge.setBorderWidth(3f);
        badge.setHorizontalAlignment(Element.ALIGN_CENTER);
        badge.setVerticalAlignment(Element.ALIGN_MIDDLE);
        badge.setPadding(10);
        titre.addCell(badge);
        titre.setSpacingAfter(10);
        doc.add(titre);
    }

    private static void bandeauReferences(Document doc, DemandeService demande, PdfStyleFacture st) {
        PdfPTable meta = new PdfPTable(new float[]{1.4f, 1.2f, 2.6f});
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(10);
        meta.addCell(PdfComposants.faMeta(st, "N° DEMANDE",
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
        String nom = PdfFormats.valeur(demande.getCommercial().getNomCommercial());
        String immatricule = demande.getCommercial().getImmatricule();
        if (immatricule != null && !immatricule.isBlank()) {
            nom += "  ·  N° IM " + immatricule;
        }
        return nom;
    }

    private static void cartesSociete(Document doc, DemandeService demande, PdfStyleFacture st) {
        Societe societe = demande.getSociete();
        PdfPCell entreprise = PdfComposants.faCarte(st, "Entreprise", new String[][]{
                {"Société", PdfFormats.valeur(societe != null ? societe.getNomSociete() : null)},
                {"Activités", PdfFormats.valeur(societe != null ? societe.getActivite() : null)},
                {"Adresse", PdfFormats.valeur(societe != null ? societe.getAdresse() : null)}
        });
        PdfPCell contact = PdfComposants.faCarte(st, "Responsable & contact", new String[][]{
                {"Responsable", PdfFormats.valeur(demande.getClientResponsable() != null
                        ? demande.getClientResponsable().getNomClient() : null)},
                {"Téléphone", PdfFormats.valeur(societe != null ? societe.getContact() : null)},
                {"Mail", PdfFormats.valeur(societe != null ? societe.getEmail() : null)},
                {"Facebook", facebook(societe)}
        });
        PdfComposants.rangee(doc, 0.3f, entreprise, contact);
    }

    private static String facebook(Societe societe) {
        return societe != null && societe.getFb() != null && !societe.getFb().isBlank()
                ? societe.getFb() : "-";
    }

    private static void profilChiffre(Document doc, Societe societe, PdfStyleFacture st) {
        String duree = PdfFormats.dureeExistence(societe);
        PdfPTable profil = new PdfPTable(3);
        profil.setWidthPercentage(100);
        profil.setSpacingBefore(6);
        profil.addCell(PdfComposants.faMeta(st, "DURÉE D'EXISTENCE",
                duree.isEmpty() ? "-" : duree, PdfTheme.PRIMAIRE));
        profil.addCell(PdfComposants.faMeta(st, "PERSONNEL",
                societe != null && societe.getNombrePersonnel() != null
                        ? societe.getNombrePersonnel().toString() : "-", PdfTheme.PRIMAIRE));
        profil.addCell(PdfComposants.faMeta(st, "CHIFFRE D'AFFAIRES MENSUEL",
                societe != null && societe.getChiffreAffaireMensuel() != null
                        ? PdfFormats.formatArgent(societe.getChiffreAffaireMensuel()) : "-",
                PdfTheme.PRIMAIRE));
        doc.add(profil);
    }

    private static final Font F_CASE_COCHEE =
            new Font(Font.ZAPFDINGBATS, 9.5f, Font.NORMAL, Color.WHITE);
    private static final Font F_DOMAINE = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, PdfTheme.TEXTE);
    private static final Font F_DOMAINE_COCHE =
            new Font(Font.HELVETICA, 9.5f, Font.BOLD, PdfTheme.PRIMAIRE);

    /**
     * Cases de domaine façon panneau de formulaire : une ligne par domaine, puce
     * verte pleine et libellé mis en avant lorsqu'il est coché, case blanche sinon.
     * Liste dynamique (tous les domaines en base, triés) ; plusieurs domaines
     * peuvent être cochés quand des services de domaines différents sont demandés.
     */
    private static void domaine(Document doc, DemandeService demande,
                                List<DomaineService> domaines) {
        PdfComposants.section(doc, "Domaine");
        Set<Integer> demandes = domainesDemandes(demande);
        if (domaines.isEmpty()) {
            Paragraph p = new Paragraph(new Chunk("-", PdfTheme.F_VALEUR));
            p.setSpacingBefore(3);
            doc.add(p);
            return;
        }
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.085f, 0.915f});
        table.setSpacingBefore(6);
        table.setSpacingAfter(4);
        for (DomaineService domaine : domaines) {
            boolean coche = demandes.contains(domaine.getId());
            table.addCell(celluleCase(coche));
            table.addCell(celluleLibelle(domaine.getNomDomaine(), coche));
        }
        doc.add(table);
    }

    /** Puce de case à cocher : pleine verte avec « ✔ » si cochée, blanche sinon. */
    private static PdfPCell celluleCase(boolean coche) {
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(17f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(2f);
        if (coche) {
            c.setBackgroundColor(PdfTheme.VERT_MARQUE);
            c.setBorderColor(PdfTheme.VERT_MARQUE);
            c.setBorderWidth(0.75f);
            Paragraph cocheP = new Paragraph(new Phrase("4", F_CASE_COCHEE));
            cocheP.setAlignment(Element.ALIGN_CENTER);
            cocheP.setLeading(9f);
            c.addElement(cocheP);
            return c;
        }
        c.setBackgroundColor(Color.WHITE);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBorderWidth(0.9f);
        Paragraph videP = new Paragraph(new Phrase(" ", F_CASE_COCHEE));
        videP.setAlignment(Element.ALIGN_CENTER);
        videP.setLeading(9f);
        c.addElement(videP);
        return c;
    }

    /** Libellé du domaine : fond pâle et texte vert foncé si coché, blanc sinon. */
    private static PdfPCell celluleLibelle(String nom, boolean coche) {
        PdfPCell c = new PdfPCell();
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderWidthBottom(0.5f);
        c.setBorderColor(PdfTheme.GRIS_BORDURE);
        c.setBackgroundColor(coche ? PdfTheme.VERT_PALE : Color.WHITE);
        c.setPaddingTop(6f);
        c.setPaddingBottom(6f);
        c.setPaddingLeft(12f);
        c.setPaddingRight(8f);
        Paragraph p = new Paragraph(new Phrase(PdfFormats.valeur(nom),
                coche ? F_DOMAINE_COCHE : F_DOMAINE));
        p.setSpacingBefore(0f);
        p.setSpacingAfter(0f);
        p.setLeading(12f);
        c.addElement(p);
        return c;
    }

    /** Type de la ligne : PRESTATION / ABONNEMENT / DEBOURS. */
    private static String typeService(LigneService ligne) {
        if (ligne.getCategorie() != null && "DEBOURS".equalsIgnoreCase(ligne.getCategorie().trim())) {
            return "Débours";
        }
        String typePrestation = ligne.getTypePrestation();
        if (typePrestation == null || typePrestation.isBlank()) {
            return "Prestation";
        }
        if ("ABONNEMENT_MENSUEL".equalsIgnoreCase(typePrestation.trim())) {
            return "Abonnement";
        }
        if ("PRESTATION".equalsIgnoreCase(typePrestation.trim())) {
            return "Prestation";
        }
        return typePrestation;
    }

    /** Libellé de la prestation : service catalogue, ou désignation du débours. */
    private static String libelleLigne(LigneService ligne) {
        if (ligne.getService() != null && ligne.getService().getNomService() != null
                && !ligne.getService().getNomService().isBlank()) {
            return ligne.getService().getNomService();
        }
        if (ligne.getDesignationDebours() != null
                && !ligne.getDesignationDebours().isBlank()) {
            return ligne.getDesignationDebours();
        }
        return "-";
    }

    private static void services(Document doc, DemandeService demande, PdfStyleFacture st) {
        PdfComposants.section(doc, "Services souhaités");
        PdfPTable services = new PdfPTable(2);
        services.setWidthPercentage(100);
        services.setWidths(new float[]{5f, 2.4f});
        services.setHeaderRows(1);
        PdfComposants.faTh(services, st, "SERVICE", Element.ALIGN_LEFT);
        PdfComposants.faTh(services, st, "TYPE DE SERVICE", Element.ALIGN_CENTER);

        List<LigneService> lignes = lignesTriees(demande);
        if (lignes.isEmpty()) {
            PdfPCell aucun = PdfComposants.faCellule(st,
                    new Phrase("Aucun service sélectionné", st.td), Element.ALIGN_LEFT);
            aucun.setColspan(2);
            services.addCell(aucun);
        } else {
            int index = 0;
            for (LigneService ligne : lignes) {
                boolean zebra = index++ % 2 == 1;
                PdfComposants.faCorps(services, st, new Phrase(
                        PdfFormats.valeur(libelleLigne(ligne)), st.tdGras),
                        Element.ALIGN_LEFT, zebra);
                PdfComposants.faCorps(services, st, new Phrase(typeService(ligne), st.td),
                        Element.ALIGN_CENTER, zebra);
            }
        }
        doc.add(services);
    }

    private static void delai(Document doc, PdfStyleFacture st) {
        PdfPCell delai = PdfComposants.faBoite(st);
        Paragraph p = new Paragraph();
        p.add(new Chunk("DÉLAI D'EXÉCUTION SOUHAITÉ PAR LE CLIENT :  ", st.carteTitre));
        p.add(new Chunk("........................................................", st.td));
        delai.addElement(p);
        doc.add(PdfComposants.boiteFiche(delai));
    }

    private static void observation(Document doc, DemandeService demande, PdfStyleFacture st) {
        PdfPCell obs = PdfComposants.faBoite(st);
        obs.setMinimumHeight(74);
        obs.addElement(new Paragraph("OBSERVATION", st.carteTitre));
        String commentaire = demande.getCommentaire();
        if (commentaire != null && !commentaire.isBlank()) {
            Paragraph p = new Paragraph(commentaire, st.td);
            p.setSpacingBefore(3);
            obs.addElement(p);
        }
        doc.add(PdfComposants.boiteFiche(obs));
    }

    private static void signatures(Document doc, PdfStyleFacture st) {
        PdfPTable signatures = PdfComposants.signaturesStyle(st, 56f,
                new String[]{"Le Client"},
                new String[]{"Le Commercial"},
                new String[]{"Le Responsable du dossier"},
                new String[]{"Manager"});
        signatures.setSpacingBefore(12);
        doc.add(signatures);
    }

}
