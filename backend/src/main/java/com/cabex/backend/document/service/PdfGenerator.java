package com.cabex.backend.document.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SOUS_TITRE = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font ENTETE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    public byte[] recapitulatifDemande(DemandeService d) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            titre(doc, "Récapitulatif de demande");
            sousTitre(doc, "Référence : " + valeur(d.getReferenceDemande()));

            infoTable(doc, new String[][]{
                    {"Client", valeur(d.getSociete() != null ? d.getSociete().getNomSociete() : null)},
                    {"Adresse", valeur(d.getSociete() != null ? d.getSociete().getAdresse() : null)},
                    {"Contact client", valeur(d.getSociete() != null ? d.getSociete().getContact() : null)},
                    {"Responsable", valeur(d.getClientResponsable() != null ? d.getClientResponsable().getNomClient() : null)},
                    {"Commercial", valeur(d.getCommercial() != null ? d.getCommercial().getNomCommercial() : null)},
                    {"Date de demande", valeurDate(d.getDateDemande())},
                    {"Nombre de services", d.getLignes() != null ? String.valueOf(d.getLignes().size()) : "-"}
            });

            label(doc, "Services");
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 3f, 3f, 3f});
            table.setHeaderRows(1);
            enteteCellule(table, "Service");
            enteteCellule(table, "Période");
            enteteCellule(table, "Tarif");
            enteteCellule(table, "Statut");
            if (d.getLignes() != null) {
                for (com.cabex.backend.demande.entity.LigneService l : d.getLignes()) {
                    cellule(table, valeur(l.getService() != null ? l.getService().getNomService() : null));
                    cellule(table, valeurDate(l.getDebutService()) + " → " + valeurDate(l.getFinService()));
                    cellule(table, formatArgent(l.getTarif()));
                    cellule(table, valeur(l.getStatut()));
                }
            }
            doc.add(table);

            label(doc, "Informations de la demande");
            Paragraph p = new Paragraph(valeurNeOuVide(d.getCommentaire()), NORMAL);
            p.setSpacingAfter(10);
            doc.add(p);

            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de génération du récapitulatif de demande", e);
        }
        return out.toByteArray();
    }

    public byte[] recuPaiement(Paiement p) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            titre(doc, "Reçu de paiement");
            sousTitre(doc, "Référence paiement : " + valeur(p.getReferencePaiement()));

            infoTable(doc, new String[][]{
                    {"Client", valeur(p.getDemande() != null && p.getDemande().getSociete() != null
                            ? p.getDemande().getSociete().getNomSociete() : null)},
                    {"Demande", valeur(p.getDemande() != null ? p.getDemande().getReferenceDemande() : null)},
                    {"Montant", formatArgent(p.getMontantPaye())},
                    {"Date", valeurDate(p.getDatePaiement())},
                    {"Mode de paiement", valeur(p.getModePaiement() != null ? p.getModePaiement().getLibelle() : null)},
                    {"Référence virement", valeur(p.getReferenceVirement())},
                    {"Enregistré par", valeur(p.getCreatedBy() != null
                            ? p.getCreatedBy().getPrenom() + " " + p.getCreatedBy().getNom() : null)}
            });

            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de génération du reçu de paiement", e);
        }
        return out.toByteArray();
    }

    public byte[] planPaiement(ProposPaiement propos) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            titre(doc, "Plan de paiement");
            sousTitre(doc, "Demande : " + valeur(propos.getDemande() != null
                    ? propos.getDemande().getReferenceDemande() : null));

            infoTable(doc, new String[][]{
                    {"Montant total", formatArgent(propos.getMontantTotal())},
                    {"Reste à payer", formatArgent(propos.getRestePayer())},
                    {"Début", valeurDate(propos.getDateDebut())},
                    {"Fin", valeurDate(propos.getDateFin())},
                    {"Nombre d'échéances", propos.getNombreEcheances() != null
                            ? propos.getNombreEcheances().toString() : "-"}
            });

            label(doc, "Échéances");
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 3f, 3f, 3f});
            table.setHeaderRows(1);
            enteteCellule(table, "N°");
            enteteCellule(table, "Date");
            enteteCellule(table, "Montant attendu");
            enteteCellule(table, "Montant payé");
            if (propos.getEcheances() != null) {
                for (EcheancePaiement e : propos.getEcheances()) {
                    cellule(table, e.getNumeroEcheance() != null ? e.getNumeroEcheance().toString() : "-");
                    cellule(table, e.getDateEcheance() != null ? e.getDateEcheance().format(DATE_FMT) : "-");
                    cellule(table, formatArgent(e.getMontantAttendu()));
                    cellule(table, formatArgent(e.getMontantPaye()));
                }
            }
            doc.add(table);

            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de génération du plan de paiement", e);
        }
        return out.toByteArray();
    }

    public byte[] factureProforma(com.cabex.backend.facture.entity.Facture f) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            titre(doc, "Facture proforma");
            sousTitre(doc, "Référence : " + valeur(f.getReferenceFacture()));

            com.cabex.backend.demande.entity.DemandeService demande = f.getDemande();
            infoTable(doc, new String[][]{
                    {"Client", valeur(demande != null && demande.getSociete() != null
                            ? demande.getSociete().getNomSociete() : null)},
                    {"Demande", valeur(demande != null ? demande.getReferenceDemande() : null)},
                    {"Mode de facturation", valeur(
                            "UNIQUE".equals(f.getModeFacturation()) ? "Unique (toute la demande)" : "Par service")},
                    {"Montant total", formatArgent(f.getMontant())},
                    {"Statut", valeur(f.getStatut())},
                    {"Validée par", valeur(f.getValidePar() != null
                            ? f.getValidePar().getPrenom() + " " + f.getValidePar().getNom() : null)},
                    {"Validée le", f.getValideLe() != null
                            ? f.getValideLe().format(
                                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-"}
            });

            label(doc, "Services facturés");
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 3f, 3f, 3f});
            table.setHeaderRows(1);
            enteteCellule(table, "Service");
            enteteCellule(table, "Période");
            enteteCellule(table, "Tarif");
            enteteCellule(table, "Statut");
            if (f.getLignes() != null) {
                for (com.cabex.backend.demande.entity.LigneService l : f.getLignes()) {
                    cellule(table, valeur(l.getService() != null ? l.getService().getNomService() : null));
                    cellule(table, valeurDate(l.getDebutService()) + " → " + valeurDate(l.getFinService()));
                    cellule(table, formatArgent(l.getTarif()));
                    cellule(table, valeur(l.getStatut()));
                }
            }
            doc.add(table);

            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de génération de la facture proforma", e);
        }
        return out.toByteArray();
    }

    private void titre(Document doc, String text) {
        Paragraph p = new Paragraph(text, TITRE);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private void sousTitre(Document doc, String text) {
        Paragraph p = new Paragraph(text, SOUS_TITRE);
        p.setSpacingAfter(14);
        doc.add(p);
    }

    private void label(Document doc, String text) {
        Paragraph p = new Paragraph(text, BOLD);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void infoTable(Document doc, String[][] lignes) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 7f});
        for (String[] ligne : lignes) {
            PdfPCell k = new PdfPCell(new Phrase(ligne[0], BOLD));
            k.setPadding(6);
            k.setBackgroundColor(new java.awt.Color(238, 238, 238));
            table.addCell(k);
            table.addCell(new PdfPCell(new Phrase(ligne[1], NORMAL)));
        }
        doc.add(table);
    }

    private void enteteCellule(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, ENTETE));
        c.setBackgroundColor(new java.awt.Color(220, 220, 220));
        c.setPadding(6);
        table.addCell(c);
    }

    private void cellule(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, NORMAL));
        c.setPadding(6);
        table.addCell(c);
    }

    private String formatArgent(BigDecimal montant) {
        if (montant == null) {
            return "-";
        }
        return montant.setScale(2, RoundingMode.HALF_UP).toString() + " (MGA)";
    }

    private String valeur(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private String valeurDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String valeurNeOuVide(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
