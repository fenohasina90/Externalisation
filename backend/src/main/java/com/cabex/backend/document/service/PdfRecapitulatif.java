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

    /**
     * Cases de domaine, une par domaine actif en base (liste dynamique) ; plusieurs
     * domaines peuvent être cochés quand plusieurs services de domaines différents
     * sont demandés.
     */
    private static void domaine(Document doc, DemandeService demande,
                                List<DomaineService> domaines) {
        PdfComposants.section(doc, "Domaine");
        Set<Integer> demandes = domainesDemandes(demande);
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3);
        p.setLeading(PdfTheme.F_VALEUR.getSize() * 1.6f);
        if (domaines.isEmpty()) {
            p.add(new Chunk("-", PdfTheme.F_VALEUR));
            doc.add(p);
            return;
        }
        boolean premier = true;
        for (DomaineService domaine : domaines) {
            if (!premier) {
                p.add(new Chunk("      ", PdfTheme.F_VALEUR));
            }
            premier = false;
            boolean coche = demandes.contains(domaine.getId());
            p.add(new Chunk((coche ? "[X]  " : "[ ]  ") + PdfFormats.valeur(domaine.getNomDomaine()),
                    PdfTheme.F_VALEUR));
        }
        doc.add(p);
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
