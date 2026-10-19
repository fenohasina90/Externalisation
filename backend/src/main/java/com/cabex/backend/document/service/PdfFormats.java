package com.cabex.backend.document.service;

import com.cabex.backend.client.entity.Societe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

/**
 * Formatage des valeurs affichées dans les documents PDF
 * (montants, dates, libellés et codes métier).
 */
final class PdfFormats {

    private PdfFormats() {
    }

    static BigDecimal montantOuZero(BigDecimal montant) {
        return montant != null ? montant : BigDecimal.ZERO;
    }

    /** Montant entier au format français (séparateur de milliers = espace insécable). */
    static String formatMontant(BigDecimal montant) {
        return montant == null ? "-" : formatteur(0).format(montant);
    }

    /** Montant avec deux décimales, suivi de la devise. */
    static String formatArgent(BigDecimal montant) {
        return montant == null ? "-" : formatteur(2).format(montant) + " Ar";
    }

    private static DecimalFormat formatteur(int decimales) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator('\u00A0');
        symboles.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat(decimales == 0 ? "#,##0" : "#,##0.00", symboles);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }

    /** Valeur affichée avec un tiret de remplacement. */
    static String valeur(String texte) {
        return texte == null || texte.isBlank() ? "-" : texte;
    }

    /** Valeur brute, sans tiret (chaîne vide si absente). */
    static String valeurTexte(String texte) {
        return texte == null || texte.isBlank() ? "" : texte;
    }

    static String valeurDate(LocalDate date) {
        return date == null ? "-" : date.format(PdfTheme.DATE_FMT);
    }

    static String valeurNombre(Integer nombre) {
        return nombre != null ? nombre.toString() : "";
    }

    static String nomComplet(String prenom, String nom) {
        String resultat = (valeurTexte(prenom) + " " + valeurTexte(nom)).trim();
        return resultat.isEmpty() ? null : resultat;
    }

    /** Durée d'existence calculée depuis la date de création de la société. */
    static String dureeExistence(Societe societe) {
        if (societe == null || societe.getDateCreation() == null) {
            return "";
        }
        int ans = Period.between(societe.getDateCreation(), LocalDate.now()).getYears();
        return ans + (ans > 1 ? " ans" : " an")
                + " (depuis le " + societe.getDateCreation().format(PdfTheme.DATE_FMT) + ")";
    }

    static String code(String valeur) {
        return valeur != null ? valeur.trim().toUpperCase(Locale.FRANCE) : "";
    }

    static boolean estValeur(String selection, String code) {
        return selection != null && selection.equals(code);
    }
}
