package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.paiement.entity.Paiement;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Reçu de paiement.
 *
 * <p>Même charte graphique que le cabEx : titre en tête, bandeau vert, cartes
 * « Client et demande », « Service et facture », « Règlement » et
 * « Montants de la facture », montant en lettres, signatures.</p>
 * <p>Le reçu affiche uniquement la référence de la facture validée liée
 * directement à ce paiement.</p>
 */
final class PdfRecuPaiement {

    private PdfRecuPaiement() {
    }

    static byte[] generer(Paiement paiement, BigDecimal sommePayeeSurFacture) {
        return PdfDocument.generer(PdfDocument.MiseEnPage.RECU_PAIEMENT, "Reçu de paiement",
                "Reçu de paiement " + PdfFormats.valeur(paiement.getReferencePaiement()),
                doc -> ecrire(doc, paiement, sommePayeeSurFacture),
                "Erreur de génération du reçu de paiement");
    }

    private static void ecrire(Document doc, Paiement paiement, BigDecimal sommePayeeSurFacture) {
        PdfComposants.titreDocument(doc, "Reçu de paiement",
                "Référence paiement : " + PdfFormats.valeur(paiement.getReferencePaiement()));
        doc.add(bandeauMontant(paiement));
        cartes(doc, paiement, sommePayeeSurFacture);
        montantEnLettres(doc, paiement);
        signatures(doc, paiement);
    }

    private static PdfPTable bandeauMontant(Paiement paiement) {
        PdfPCell montant = new PdfPCell();
        montant.setBackgroundColor(PdfTheme.PRIMAIRE);
        montant.setBorder(Rectangle.NO_BORDER);
        montant.setPadding(16);

        Paragraph libelle = new Paragraph("MONTANT REÇU", PdfTheme.F_LABEL_BLANC);
        libelle.setAlignment(Element.ALIGN_CENTER);
        montant.addElement(libelle);

        Paragraph valeur = new Paragraph(PdfFormats.formatArgent(paiement.getMontantPaye()),
                PdfTheme.F_MONTANT_XL);
        valeur.setAlignment(Element.ALIGN_CENTER);
        valeur.setSpacingBefore(4);
        montant.addElement(valeur);

        PdfPTable bandeau = new PdfPTable(1);
        bandeau.setWidthPercentage(100);
        bandeau.setSpacingAfter(14);
        bandeau.addCell(montant);
        return bandeau;
    }

    // ------------------------------------------------------------------
    // Cartes
    // ------------------------------------------------------------------

    private static void cartes(Document doc, Paiement paiement, BigDecimal sommePayeeSurFacture) {
        DemandeService demande = paiement.getDemande();
        FactureValidee fv = paiement.getFactureValidee();
        Societe societe = demande != null ? demande.getSociete() : null;

        String type = typeFactureValidee(fv);
        String objetLabel = objetLabelFor(type);
        String objetValeur = objetValeurFor(type, fv);

        // Rangée 1 : client / service
        PdfPCell cClient = PdfComposants.carte("Client et demande", new String[][]{
                {"Référence paiement", PdfFormats.valeur(paiement.getReferencePaiement())},
                {"Client", PdfFormats.valeur(societe != null ? societe.getNomSociete() : null)},
                {"Demande", PdfFormats.valeur(demande != null ? demande.getReferenceDemande() : null)},
                {"Date de paiement", PdfFormats.valeurDate(paiement.getDatePaiement())}
        });
        boolean mensuelle = fv != null && fv.getMois() != null;
        java.util.List<String[]> lignesService = new java.util.ArrayList<>();
        lignesService.add(new String[]{objetLabel, PdfFormats.valeur(objetValeur)});
        lignesService.add(new String[]{"Référence facture", PdfFormats.valeur(libelleFacture(fv))});
        if (mensuelle) {
            lignesService.add(new String[]{"Mois facturé", "Abonnement " + moisLabel(fv.getMois())});
            lignesService.add(new String[]{"Date d'emission", PdfFormats.valeurDate(
                    fv.getValideLe() != null ? fv.getValideLe().toLocalDate() : null)});
        }
        PdfPCell cService = PdfComposants.carte("Service et facture",
                lignesService.toArray(new String[0][]));
        PdfComposants.rangee(doc, 0.5f, cClient, cService);

        // Rangée 2 : règlement / montants
        PdfPCell cReglement = PdfComposants.carte("Règlement", new String[][]{
                {"Mode de paiement", PdfFormats.valeur(paiement.getModePaiement() != null
                        ? paiement.getModePaiement().getLibelle() : null)},
                {"Référence virement", referenceVirementPour(paiement)},
                {"N° de chèque", numeroChequePour(paiement)},
                {"Banque", banquePour(paiement)}
        });
        PdfPCell cMontants = PdfComposants.carte("Montants de la facture", new String[][]{
                {"Montant facture validée", PdfFormats.formatArgent(montantFacture(fv))},
                {"Montant déjà payé", PdfFormats.formatArgent(montantDejaPaye(fv, sommePayeeSurFacture))},
                {"Montant reçu", PdfFormats.formatArgent(paiement.getMontantPaye())},
                {"Reste à payer", PdfFormats.formatArgent(reste(fv, sommePayeeSurFacture))}
        });
        PdfComposants.rangee(doc, 0.5f, cReglement, cMontants);
    }

    private static String typeFactureValidee(FactureValidee fv) {
        if (fv == null) {
            return "PRESTATION";
        }
        if (fv.getMois() != null) {
            return "ABONNEMENT_MENSUEL";
        }
        LigneService ligne = fv.getLigneService();
        if (ligne != null && com.cabex.backend.demande.service.LigneServiceService.estDebours(ligne)) {
            return "DEBOURS";
        }
        return "PRESTATION";
    }

    private static String objetLabelFor(String type) {
        switch (type) {
            case "DEBOURS":
                return "Désignation du débours";
            case "ABONNEMENT_MENSUEL":
                return "Nom du service";
            case "PRESTATION":
            default:
                return "Nom du service";
        }
    }

    private static String objetValeurFor(String type, FactureValidee fv) {
        if (fv == null) {
            return null;
        }
        LigneService ligne = fv.getLigneService();
        if (ligne == null) {
            return fv.getReferenceFactureValidee();
        }
        if ("DEBOURS".equals(type)) {
            String d = ligne.getDesignationDebours();
            return d != null && !d.isBlank() ? d.trim() : com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(ligne);
        }
        String nomService = ligne.getService() != null ? ligne.getService().getNomService() : null;
        return nomService != null && !nomService.isBlank() ? nomService.trim()
                : com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(ligne);
    }

    private static String libelleFacture(FactureValidee fv) {
        if (fv == null) {
            return null;
        }
        if (fv.getReferenceFactureValidee() != null) {
            return fv.getReferenceFactureValidee();
        }
        return fv.getFactureProforma() != null
                ? fv.getFactureProforma().getReferenceFacture() : null;
    }

    private static BigDecimal montantFacture(FactureValidee fv) {
        return fv != null && fv.getMontant() != null ? fv.getMontant() : null;
    }

    private static BigDecimal montantDejaPaye(FactureValidee fv, BigDecimal sommePayeeSurFacture) {
        if (sommePayeeSurFacture != null) {
            return sommePayeeSurFacture;
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal reste(FactureValidee fv, BigDecimal sommePayeeSurFacture) {
        BigDecimal montant = montantFacture(fv);
        if (montant == null) {
            return null;
        }
        BigDecimal payee = sommePayeeSurFacture != null ? sommePayeeSurFacture : BigDecimal.ZERO;
        return montant.subtract(payee).max(BigDecimal.ZERO);
    }

    private static String moisLabel(java.time.LocalDate mois) {
        if (mois == null) {
            return null;
        }
        String s = java.time.format.DateTimeFormatter
                .ofPattern("MMMM yyyy", Locale.FRANCE).format(mois);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String referenceVirementPour(Paiement paiement) {
        String mode = paiement.getModePaiement() != null
                ? paiement.getModePaiement().getCode() : null;
        return mode != null && mode.equalsIgnoreCase("VIREMENT")
                ? PdfFormats.valeur(paiement.getReferenceVirement()) : "—";
    }

    private static String numeroChequePour(Paiement paiement) {
        String mode = paiement.getModePaiement() != null
                ? paiement.getModePaiement().getCode() : null;
        return mode != null && mode.equalsIgnoreCase("CHEQUE")
                ? PdfFormats.valeur(paiement.getNumeroCheque()) : "—";
    }

    private static String banquePour(Paiement paiement) {
        String mode = paiement.getModePaiement() != null
                ? paiement.getModePaiement().getCode() : null;
        return mode != null && mode.equalsIgnoreCase("CHEQUE")
                ? PdfFormats.valeur(paiement.getBanque()) : "—";
    }

    // ------------------------------------------------------------------
    // Montant en lettres et signatures
    // ------------------------------------------------------------------

    private static void montantEnLettres(Document doc, Paiement paiement) {
        if (paiement.getMontantPaye() == null) {
            return;
        }
        doc.add(PdfComposants.encartLettres("Arrêté le présent reçu à la somme de ",
                MontantEnLettres.convertir(paiement.getMontantPaye().longValue())
                        .toUpperCase(Locale.FRANCE) + " ARIARY"));
    }

    private static void signatures(Document doc, Paiement paiement) {
        String enregistrePar = paiement.getCreatedBy() != null
                ? PdfFormats.nomComplet(paiement.getCreatedBy().getPrenom(),
                        paiement.getCreatedBy().getNom())
                : null;

        PdfPTable signatures = new PdfPTable(2);
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(0);
        signatures.setKeepTogether(true);
        signatures.setWidths(new float[]{1f, 1f});
        signatures.addCell(PdfComposants.celluleSignature("Le client", null));
        signatures.addCell(PdfComposants.celluleSignature("Pour le cabinet",
                enregistrePar != null ? "Enregistré par " + enregistrePar : null));
        signatures.addCell(PdfComposants.celluleEspaceSignature(70));
        signatures.addCell(PdfComposants.celluleEspaceSignature(70));
        doc.add(signatures);
    }
}
