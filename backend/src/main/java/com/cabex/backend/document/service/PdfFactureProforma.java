package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.facture.entity.Facture;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Facture proforma au format du modèle CABEX, toujours générée sur une seule page.
 */
final class PdfFactureProforma {

    private final ConfigurationService configurationService;

    PdfFactureProforma(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Génère le PDF avec l'en-tête demandé :
     * « FACTURE PROFORMA » pour le document proforma (avant ou après validation),
     * « FACTURE VALIDÉE » pour le document issu de la validation.
     */
    byte[] generer(Facture facture, String contexte) {
        byte[] pdf = null;
        String titre = "VALIDÉE".equals(contexte) ? "VALIDÉE" : "PROFORMA";
        for (float echelle : PdfStyleFacture.ECHELLES) {
            pdf = construire(facture, new PdfStyleFacture(echelle), contexte);
            if (PdfDocument.nombrePages(pdf) <= 1) {
                break;
            }
        }
        return pdf;
    }

    private byte[] construire(Facture facture, PdfStyleFacture st, String contexte) {
        String titreBadge = "VALIDÉE".equals(contexte) ? "VALIDÉE" : "PROFORMA";
        String titreMeta = "Facture " + titreBadge.toLowerCase(java.util.Locale.FRANCE) + " "
                + PdfFormats.valeur(facture.getReferenceFacture());
        return PdfDocument.generer(PdfDocument.MiseEnPage.FACTURE, "Facture " + titreBadge.toLowerCase(java.util.Locale.FRANCE),
                titreMeta, doc -> ecrire(doc, facture, st, contexte),
                "Erreur de génération de la facture proforma");
    }

    private void ecrire(Document doc, Facture facture, PdfStyleFacture st, String contexte) {
        DemandeService demande = facture.getDemande();
        Societe societe = demande != null ? demande.getSociete() : null;
        List<LigneAffiche> lignesAffiches = construireLignesAffiches(facture, demande, contexte);

        entete(doc, facture, st, contexte);
        destinataire(doc, demande, societe, st);

        BigDecimal totalHt = lignesAffiches.stream()
                .map(LigneAffiche::netCommercial)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        doc.add(tableauPrestations(lignesAffiches, facture, st, totalHt));

        BigDecimal tauxTva = lireTauxTva();
        BigDecimal tva = totalHt.multiply(tauxTva)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal ttc = totalHt.add(tva);
        BigDecimal avance = avanceRecue(facture);
        BigDecimal netAPayer = ttc.subtract(avance).max(BigDecimal.ZERO);

        doc.add(blocBas(demande, st, totalHt, tauxTva, tva, ttc, avance, netAPayer));
        doc.add(PdfFactureComposants.conditionsReglement(st));
        doc.add(signatures(st));
    }

    // ------------------------------------------------------------------
    // En-tête et destinataire
    // ------------------------------------------------------------------

    private static void entete(Document doc, Facture facture, PdfStyleFacture st, String contexte) {
        String sousTitre = "VALIDÉE".equals(contexte) ? "VALIDÉE" : "PROFORMA";
        PdfComposants.enteteDocument(doc, st, "FACTURE", sousTitre);

        DemandeService demande = facture.getDemande();
        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(st.ecart * 0.8f);
        meta.addCell(PdfComposants.faMeta(st, "N° FACTURE",
                PdfFormats.valeur(facture.getReferenceFacture()), PdfTheme.ROUGE));
        meta.addCell(PdfComposants.faMeta(st, "DATE DEMANDE",
                PdfFormats.valeurDate(demande != null ? demande.getDateDemande() : null),
                PdfTheme.PRIMAIRE));
        meta.addCell(PdfComposants.faMeta(st, "DEMANDE",
                PdfFormats.valeur(demande != null ? demande.getReferenceDemande() : null),
                PdfTheme.PRIMAIRE));
        doc.add(meta);
    }

    private static void destinataire(Document doc, DemandeService demande, Societe societe,
                                     PdfStyleFacture st) {
        PdfPCell cDest = PdfComposants.faCarte(st, "Facturé à", new String[][]{
                {"Raison sociale", PdfFormats.valeur(societe != null ? societe.getNomSociete() : null)},
                {"Adresse", PdfFormats.valeur(societe != null ? societe.getAdresse() : null)},
                {"Contact", PdfFormats.valeur(societe != null ? societe.getContact() : null)},
                {"Mail", PdfFormats.valeur(societe != null ? societe.getEmail() : null)},
                {"NIF / STAT / RCS", "..................  /  ..................  /  .................."}
        });
        PdfPCell cRef = PdfComposants.faCarte(st, "Informations", new String[][]{
                {"Responsable", PdfFormats.valeur(demande != null
                        && demande.getClientResponsable() != null
                        ? demande.getClientResponsable().getNomClient() : null)},
                {"Demande", PdfFormats.valeur(demande != null ? demande.getReferenceDemande() : null)},
                {"Règlement", "À payer dès la réception"}
        });
        PdfComposants.rangee(doc, 0.3f, cDest, cRef);
    }

    // ------------------------------------------------------------------
    // Tableau des prestations
    // ------------------------------------------------------------------

    /** Ligne générique pour l'affichage (voie persistée ou voie historique). */
    private record LigneAffiche(
            String categorie,
            String typePrestation,
            String designation,
            BigDecimal pu,
            BigDecimal quantite,
            BigDecimal montantHtva,
            BigDecimal remise,
            BigDecimal netCommercial
    ) {
    }

    /**
     * Lignes affichées dans le PDF.
     * Voie principale : lignes persistées dans la proforma au moment de la
     * génération — le contenu est figé (désignation, PU, quantité, HTVA, remise,
     * net commercial). Voie de repli pour les anciennes données : les lignes de
     * la proforma et les débours de la demande.
     */
    private static List<LigneAffiche> construireLignesAffiches(Facture facture, DemandeService demande,
                                                              String contexte) {
        boolean estValidee = "VALIDÉE".equals(contexte);
        List<com.cabex.backend.facture.entity.FactureProformaLigne> persistees =
                facture.getLignesProforma();
        if (persistees != null && !persistees.isEmpty()) {
            return persistees.stream()
                    .sorted(java.util.Comparator.comparing(
                            com.cabex.backend.facture.entity.FactureProformaLigne::getOrdre,
                            java.util.Comparator.nullsLast(Integer::compareTo)))
                    .map(l -> {
                        BigDecimal puArbitre;
                        if (estValidee && l.getLigneService() != null && l.getLigneService().getTarif() != null) {
                            puArbitre = l.getLigneService().getTarif();
                        } else {
                            puArbitre = l.getPu();
                        }
                        return new LigneAffiche(
                                l.getCategorie(),
                                l.getTypePrestation(),
                                l.getDesignation(),
                                PdfFormats.montantOuZero(puArbitre),
                                PdfFormats.montantOuZero(l.getQuantite()),
                                PdfFormats.montantOuZero(puArbitre),
                                PdfFormats.montantOuZero(l.getRemise()),
                                PdfFormats.montantOuZero(puArbitre));
                    })
                    .toList();
        }
        List<LigneService> lignes = new java.util.ArrayList<>();
        if (facture.getLignes() != null) {
            lignes.addAll(facture.getLignes());
        }
        if (demande != null && demande.getLignes() != null) {
            for (LigneService l : demande.getLignes()) {
                if (com.cabex.backend.demande.service.LigneServiceService.estDebours(l)
                        && l.getMontantDebours() != null
                        && lignes.stream().noneMatch(x -> x.getId() != null && x.getId().equals(l.getId()))) {
                    lignes.add(l);
                }
            }
        }
        return lignes.stream()
                .map(l -> new LigneAffiche(
                        l.getCategorie(),
                        l.getTypePrestation(),
                        designationLigne(l),
                        PdfFormats.montantOuZero(tarifAffichePdf(l, facture)),
                        BigDecimal.ONE,
                        PdfFormats.montantOuZero(tarifAffichePdf(l, facture)),
                        BigDecimal.ZERO,
                        PdfFormats.montantOuZero(tarifAffichePdf(l, facture))))
                .toList();
    }

    private static String designationLigne(LigneService ligne) {
        if (com.cabex.backend.demande.service.LigneServiceService.estDebours(ligne)) {
            String d = ligne.getDesignationDebours();
            return d != null && !d.isBlank() ? d.trim() : "Débours";
        }
        String nomService = ligne.getService() != null ? ligne.getService().getNomService() : null;
        return (nomService != null && !nomService.isBlank())
                ? nomService.trim()
                : com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(ligne);
    }

    private static PdfPTable tableauPrestations(List<LigneAffiche> lignesPdf, Facture facture,
                                                PdfStyleFacture st, BigDecimal totalHt) {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.4f, 1.5f, 0.7f, 1.9f, 1.3f, 1.9f});
        table.setHeaderRows(1);
        table.setSpacingBefore(st.ecart);
        PdfComposants.faTh(table, st, "DÉSIGNATION", Element.ALIGN_LEFT);
        PdfComposants.faTh(table, st, "PU\n(Ariary)", Element.ALIGN_RIGHT);
        PdfComposants.faTh(table, st, "QTÉ", Element.ALIGN_CENTER);
        PdfComposants.faTh(table, st, "MONTANT HTVA\n(Ariary)", Element.ALIGN_RIGHT);
        PdfComposants.faTh(table, st, "REMISE\n(Ariary)", Element.ALIGN_RIGHT);
        PdfComposants.faTh(table, st, "NET COMMERCIAL\n(Ariary)", Element.ALIGN_RIGHT);

        List<LigneAffiche> abonnements = new java.util.ArrayList<>();
        List<LigneAffiche> prestations = new java.util.ArrayList<>();
        List<LigneAffiche> debours = new java.util.ArrayList<>();
        for (LigneAffiche ligne : lignesPdf) {
            if (com.cabex.backend.demande.service.LigneServiceService.CATEGORIE_DEBOURS
                    .equalsIgnoreCase(ligne.categorie())) {
                debours.add(ligne);
            } else if (com.cabex.backend.demande.service.LigneServiceService.ABONNEMENT_MENSUEL
                    .equalsIgnoreCase(ligne.typePrestation())) {
                abonnements.add(ligne);
            } else {
                prestations.add(ligne);
            }
        }

        int zebra = 0;
        zebra = ajouterSection(table, st, facture, "Abonnement mensuel :",
                "Sous-total Abonnement mensuel", abonnements, zebra);
        zebra = ajouterSection(table, st, facture, "Prestation:",
                "Sous-total Prestation", prestations, zebra);
        zebra = ajouterSection(table, st, facture, "Débours à payer à l'administration",
                "Sous-total Débours", debours, zebra);

        PdfComposants.faSousTotal(table, st, "TOTAL HT", 3);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(totalHt), 1);
        PdfComposants.faSousTotal(table, st, "-", 1);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(totalHt), 1);
        return table;
    }

    /**
     * Ajoute une section de la facture (bande, lignes, sous-total).
     * La section n'est insérée que s'il y a au moins une ligne.
     * Retourne le prochain index zebrame.
     */
    private static int ajouterSection(PdfPTable table, PdfStyleFacture st,
                                      Facture facture, String titre, String sousTotal,
                                      List<LigneAffiche> lignes, int zebraDepart) {
        if (lignes.isEmpty()) {
            return zebraDepart;
        }
        PdfPCell bande = PdfComposants.faCellule(st,
                new Phrase(titre, st.section), Element.ALIGN_LEFT);
        bande.setColspan(6);
        bande.setBackgroundColor(PdfTheme.VERT_PALE);
        bande.setBorder(Rectangle.LEFT);
        bande.setBorderWidthLeft(2.5f);
        bande.setBorderColorLeft(PdfTheme.VERT_MARQUE);
        table.addCell(bande);
        BigDecimal somme = BigDecimal.ZERO;
        int zebra = zebraDepart;
        for (LigneAffiche ligne : lignes) {
            BigDecimal montant = PdfFormats.montantOuZero(ligne.netCommercial());
            somme = somme.add(montant);
            lignesPrestation(table, st, facture, ligne, zebra++ % 2 == 1);
        }
        PdfComposants.faSousTotal(table, st, sousTotal, 3);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(somme), 1);
        PdfComposants.faSousTotal(table, st, "-", 1);
        PdfComposants.faSousTotal(table, st, PdfFormats.formatMontant(somme), 1);
        return zebra;
    }

    private static void lignesPrestation(PdfPTable table, PdfStyleFacture st,
                                         Facture facture, LigneAffiche ligne, boolean zebra) {
        Phrase designation = new Phrase();
        designation.setLeading(st.td.getSize() * 1.3f);
        designation.add(new Chunk(PdfFormats.valeur(ligne.designation()), st.tdGras));
        PdfComposants.faCorps(table, st, designation, Element.ALIGN_LEFT, zebra);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(ligne.pu()), st.td),
                Element.ALIGN_RIGHT, zebra);
        PdfComposants.faCorps(table, st, new Phrase(formatQuantite(ligne.quantite()), st.td), Element.ALIGN_CENTER, zebra);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(ligne.montantHtva()), st.td),
                Element.ALIGN_RIGHT, zebra);
        PdfComposants.faCorps(table, st, new Phrase(formateRemiseOuTiret(ligne.remise()), st.td),
                Element.ALIGN_RIGHT, zebra);
        PdfComposants.faCorps(table, st, new Phrase(PdfFormats.formatMontant(ligne.netCommercial()), st.td),
                Element.ALIGN_RIGHT, zebra);
    }

    private static String formatQuantite(BigDecimal qte) {
        BigDecimal entier = qte.stripTrailingZeros();
        return entier.scale() <= 0 ? entier.toPlainString() : entier.toPlainString();
    }

    private static String formateRemiseOuTiret(BigDecimal remise) {
        return remise == null || remise.signum() == 0
                ? "-"
                : PdfFormats.formatMontant(remise);
    }

    /**
     * Tarif affiché dans le PDF : après validation, le document validée
     * affiche les montants validés (tarif ligne) ; la proforma garde
     * le tarif figé (tarif_proforma). Pour un débours : le montant annoncé
     * à la création (figé).
     */
    private static BigDecimal tarifAffichePdf(LigneService ligne, Facture facture) {
        if (com.cabex.backend.demande.service.LigneServiceService.estDebours(ligne)) {
            return ligne.getMontantDebours() != null ? ligne.getMontantDebours() : BigDecimal.ZERO;
        }
        if (facture.getStatut() != null && "VALIDEE".equalsIgnoreCase(facture.getStatut())) {
            return ligne.getTarif() != null ? ligne.getTarif() : BigDecimal.ZERO;
        }
        return ligne.getTarifProforma() != null ? ligne.getTarifProforma()
                : (ligne.getTarif() != null ? ligne.getTarif() : BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------
    // Totaux, montant en lettres et observations
    // ------------------------------------------------------------------

    private static PdfPTable blocBas(DemandeService demande, PdfStyleFacture st,
                                     BigDecimal totalHt, BigDecimal tauxTva, BigDecimal tva,
                                     BigDecimal ttc, BigDecimal avance, BigDecimal netAPayer) {
        PdfPTable gauche = new PdfPTable(1);
        gauche.setWidthPercentage(100);
        gauche.addCell(PdfComposants.celluleLettres("Arrêtée la présente facture à la somme de",
                MontantEnLettres.convertir(netAPayer.longValue()).toUpperCase(Locale.FRANCE)
                        + " ARIARY"));
        PdfPCell espace = PdfComposants.celluleVide();
        espace.setFixedHeight(st.ecart * 0.6f);
        gauche.addCell(espace);
        gauche.addCell(observations(demande, st));

        PdfPTable totaux = new PdfPTable(2);
        totaux.setWidthPercentage(100);
        totaux.setWidths(new float[]{5f, 4.6f});
        PdfComposants.faLigneTotaux(totaux, st, "TVA" + (tauxTva.signum() > 0
                        ? " (" + tauxTva.stripTrailingZeros().toPlainString() + " %)" : ""),
                PdfFormats.formatMontant(tva), 0);
        PdfComposants.faLigneTotaux(totaux, st, "TOTAL TTC",
                PdfFormats.formatMontant(ttc), 1);
        PdfComposants.faLigneTotaux(totaux, st, "Avance reçue",
                PdfFormats.formatMontant(avance), 0);
        PdfComposants.faLigneTotaux(totaux, st, "NET À PAYER",
                PdfFormats.formatMontant(netAPayer), 2);

        PdfPTable bas = new PdfPTable(3);
        bas.setWidthPercentage(100);
        bas.setWidths(new float[]{5.4f, 0.35f, 4.25f});
        bas.setSpacingBefore(st.ecart);
        bas.setKeepTogether(true);
        bas.addCell(celluleSansBordure(gauche));
        bas.addCell(PdfComposants.celluleVide());
        bas.addCell(celluleSansBordure(totaux));
        return bas;
    }

    private static PdfPCell observations(DemandeService demande, PdfStyleFacture st) {
        PdfPCell obs = new PdfPCell();
        obs.setBorderColor(PdfTheme.GRIS_BORDURE);
        obs.setBorderWidth(0.5f);
        obs.setPadding(st.padCarte);
        obs.setMinimumHeight(st.hObservations);
        obs.addElement(new Paragraph("OBSERVATIONS", st.carteTitre));
        String commentaire = demande != null ? PdfFormats.valeurTexte(demande.getCommentaire()) : "";
        if (!commentaire.isEmpty()) {
            Paragraph p = new Paragraph(commentaire, st.petit);
            p.setSpacingBefore(2);
            obs.addElement(p);
        }
        return obs;
    }

    private static PdfPCell celluleSansBordure(PdfPTable contenu) {
        PdfPCell c = new PdfPCell(contenu);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    // ------------------------------------------------------------------
    // Signatures
    // ------------------------------------------------------------------

    private static PdfPTable signatures(PdfStyleFacture st) {
        PdfPTable signatures = PdfComposants.signaturesStyle(st, st.hSignature,
                new String[]{"Client"},
                new String[]{"Responsable Traitement", "Dossiers Clients"},
                new String[]{"Responsable Audit", "Contrôleur de Gestion"},
                new String[]{"Manager"});
        signatures.setSpacingBefore(st.ecart);
        return signatures;
    }

    // ------------------------------------------------------------------
    // Calculs
    // ------------------------------------------------------------------

    private BigDecimal lireTauxTva() {
        try {
            String valeur = configurationService.lireValeur("TAUX_TVA", "0");
            return new BigDecimal(valeur.trim()).max(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** Avance déjà reçue : somme payée sur les échéances des lignes facturées. */
    private static BigDecimal avanceRecue(Facture facture) {
        BigDecimal avance = BigDecimal.ZERO;
        if (facture.getLignes() == null) {
            return avance;
        }
        for (LigneService ligne : facture.getLignes()) {
            if (ligne.getPlansPaiement() == null) {
                continue;
            }
            for (ProposPaiement plan : ligne.getPlansPaiement()) {
                if (plan.getEcheances() == null) {
                    continue;
                }
                for (EcheancePaiement echeance : plan.getEcheances()) {
                    avance = avance.add(PdfFormats.montantOuZero(echeance.getMontantPaye()));
                }
            }
        }
        return avance;
    }
}
