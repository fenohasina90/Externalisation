package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
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
import java.util.Locale;

/**
 * Fiche récapitulative d'une demande d'externalisation.
 */
final class PdfRecapitulatif {

    private PdfRecapitulatif() {
    }

    static byte[] generer(DemandeService demande) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.DOCUMENT, "Fiche récapitulative",
                "Récapitulatif de demande " + PdfFormats.valeur(demande.getReferenceDemande()),
                doc -> ecrire(doc, demande),
                "Erreur de génération du récapitulatif de demande");
    }

    private static void ecrire(Document doc, DemandeService demande) {
        PdfStyleFacture st = new PdfStyleFacture(1f);
        entete(doc);
        bandeauReferences(doc, demande, st);
        cartesSociete(doc, demande, st);
        profilChiffre(doc, demande.getSociete(), st);
        domaine(doc, demande);
        services(doc, demande, st);
        delai(doc, st);
        observation(doc, demande, st);
        signatures(doc, st);
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

    private static void domaine(Document doc, DemandeService demande) {
        PdfComposants.section(doc, "Domaine");
        String domaine = "";
        if (demande.getLignes() != null) {
            for (LigneService ligne : demande.getLignes()) {
                if (ligne.getService() != null && ligne.getService().getDomaine() != null) {
                    domaine = ligne.getService().getDomaine().getNomDomaine();
                    break;
                }
            }
        }
        doc.add(casesDomaine(domaine, "Comptabilité", "Fiscale", "Audit interne"));
    }

    /** Cases [X]/[ ] cochées si le domaine contient le libellé (insensible à la casse). */
    private static Paragraph casesDomaine(String domaine, String... libelles) {
        String norme = domaine != null ? domaine.toLowerCase(Locale.FRANCE) : "";
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3);
        p.setLeading(PdfTheme.F_VALEUR.getSize() * 1.6f);
        for (int i = 0; i < libelles.length; i++) {
            if (i > 0) {
                p.add(new Chunk("      ", PdfTheme.F_VALEUR));
            }
            boolean coche = !norme.isEmpty()
                    && norme.contains(libelles[i].toLowerCase(Locale.FRANCE));
            p.add(new Chunk((coche ? "[X]  " : "[ ]  ") + libelles[i], PdfTheme.F_VALEUR));
        }
        return p;
    }

    private static void services(Document doc, DemandeService demande, PdfStyleFacture st) {
        PdfComposants.section(doc, "Services souhaités");
        PdfPTable services = new PdfPTable(3);
        services.setWidthPercentage(100);
        services.setWidths(new float[]{5f, 1.6f, 1.6f});
        services.setHeaderRows(1);
        PdfComposants.faTh(services, st, "SERVICE", Element.ALIGN_LEFT);
        PdfComposants.faTh(services, st, "DÉBUT", Element.ALIGN_CENTER);
        PdfComposants.faTh(services, st, "FIN", Element.ALIGN_CENTER);

        if (demande.getLignes() == null || demande.getLignes().isEmpty()) {
            PdfPCell aucun = PdfComposants.faCellule(st,
                    new Phrase("Aucun service sélectionné", st.td), Element.ALIGN_LEFT);
            aucun.setColspan(3);
            services.addCell(aucun);
        } else {
            int index = 0;
            for (LigneService ligne : demande.getLignes()) {
                boolean zebra = index++ % 2 == 1;
                PdfComposants.faCorps(services, st, new Phrase(PdfFormats.valeur(
                        ligne.getService() != null ? ligne.getService().getNomService() : null),
                        st.tdGras), Element.ALIGN_LEFT, zebra);
                PdfComposants.faCorps(services, st,
                        new Phrase(PdfFormats.valeurDate(ligne.getDebutService()), st.td),
                        Element.ALIGN_CENTER, zebra);
                PdfComposants.faCorps(services, st,
                        new Phrase(PdfFormats.valeurDate(ligne.getFinService()), st.td),
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
