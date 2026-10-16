package com.cabex.backend.document.service;

/**
 * Conversion d'un montant entier en toutes lettres (français),
 * ex. 21000000 → « vingt et un millions ». Gère 0 au milliard.
 */
public class MontantEnLettres {

    private static final String[] UNITES = {
            "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept",
            "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze",
            "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"};

    private static final String[] DIZAINES = {
            "", "", "vingt", "trente", "quarante", "cinquante", "soixante",
            "soixante", "quatre-vingt", "quatre-vingt"};

    private MontantEnLettres() {
    }

    public static String convertir(long montant) {
        if (montant < 0) {
            return "moins " + convertir(-montant);
        }
        if (montant < 20) {
            return UNITES[(int) montant];
        }
        if (montant < 100) {
            return dizaines((int) montant);
        }
        if (montant < 1000) {
            return centaines((int) montant);
        }
        if (montant < 1_000_000) {
            long milliers = montant / 1000;
            long reste = montant % 1000;
            String debut = milliers == 1 ? "mille" : convertir(milliers) + " mille";
            return reste == 0 ? debut : debut + " " + convertir(reste);
        }
        if (montant < 1_000_000_000) {
            long millions = montant / 1_000_000;
            long reste = montant % 1_000_000;
            String debut = convertir(millions) + (millions == 1 ? " million" : " millions");
            return reste == 0 ? debut : debut + " " + convertir(reste);
        }
        long milliards = montant / 1_000_000_000;
        long reste = montant % 1_000_000_000;
        String debut = convertir(milliards) + (milliards == 1 ? " milliard" : " milliards");
        return reste == 0 ? debut : debut + " " + convertir(reste);
    }

    private static String dizaines(int n) {
        int dizaine = n / 10;
        int unite = n % 10;
        // 70-79 : soixante-dix, soixante et onze, soixante-douze… ;
        // 90-99 : quatre-vingt-dix…
        if (dizaine == 7 || dizaine == 9) {
            if (dizaine == 7 && unite == 1) {
                return "soixante et onze";
            }
            return DIZAINES[dizaine] + "-" + UNITES[10 + unite];
        }
        if (unite == 0) {
            // 80 → « quatre-vingts », 20/30/… sans « s ».
            return dizaine == 8 ? "quatre-vingts" : DIZAINES[dizaine];
        }
        if (unite == 1 && dizaine != 8) {
            return DIZAINES[dizaine] + " et un";
        }
        return DIZAINES[dizaine] + "-" + UNITES[unite];
    }

    private static String centaines(int n) {
        int centaines = n / 100;
        int reste = n % 100;
        String debut = centaines == 1 ? "cent" : UNITES[centaines] + " cent" + (reste == 0 ? "s" : "");
        if (centaines == 1 && reste == 0) {
            return "cent";
        }
        return reste == 0 ? debut : debut + " " + convertir(reste);
    }
}
