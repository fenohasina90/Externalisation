package com.cabex.backend.exception;

/**
 * Messages d'erreur métier standardisés et lisibles.
 * Chaque service lève une exception en utilisant ces messages constants
 * afin de garantir des réponses cohérentes et compréhensibles côté frontend.
 */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    // Ressources
    public static final String RESSOURCE_INTROUVABLE = "Ressource introuvable";

    public static String introuvable(String entite) {
        return entite + " introuvable";
    }

    public static String introuvable(String entite, Object id) {
        return entite + " introuvable avec l'identifiant " + id;
    }

    // Authentication / autorisation
    public static final String IDENTIFIANTS_INCORECTS = "Identifiant ou mot de passe incorrect";
    public static final String COMPTE_DESACTIVE = "Ce compte est désactivé";
    public static final String MDP_TEMPORAIRE_EXPIRE =
            "Votre mot de passe temporaire a expiré. Veuillez demander une nouvelle réinitialisation";
    public static final String AUTH_REQUISE = "Authentification requise";
    public static final String ACCES_REFUSE = "Vous n'avez pas les permissions nécessaires pour effectuer cette action";
    public static final String TOKEN_INVALIDE = "Le token de réinitialisation est invalide ou expiré";

    // Validation métier
    public static final String DONNEES_OBLIGATOIRES = "Données obligatoires manquantes ou invalides";

    public static String champObligatoire(String champ) {
        return "Le champ '" + champ + "' est obligatoire";
    }

    public static String formatInvalide(String champ) {
        return "Le format du champ '" + champ + "' est invalide";
    }

    public static String valeurNegative(String champ) {
        return "Le champ '" + champ + "' doit être positif";
    }

    public static String referenceDejaUtilisee(String reference) {
        return "La référence '" + reference + "' est déjà utilisée";
    }

    public static final String DATE_INVALIDE = "La date fournie est invalide";
    public static final String DATE_FIN_AVANT_DEBUT = "La date de fin doit être postérieure ou égale à la date de début";
    public static final String SERVICE_INDISPONIBLE = "Ce service n'est pas disponible ou a été archivé";

    // Paiement
    public static final String PAIEMENT_SUPERIEUR_RESTE = "Le montant du paiement dépasse le montant restant";
    public static final String PAIEMENT_POSITIF = "Le montant du paiement doit être supérieur à 0";
    public static final String VIREMENT_REFERENCE_OBLIGATOIRE = "La référence du virement est obligatoire pour un paiement par virement";
    public static final String ECHEANCE_INVALIDE = "L'échéance est invalide";
    public static final String ECHEANCE_N_APPARTIENT_PAS_DEMANDE = "L'échéance n'appartient pas à cette demande";

    // Statuts
    public static final String TRANSITION_STATUT_INTERDITE = "Cette transition de statut n'est pas autorisée";
    public static final String STATUT_INVALIDE = "Le statut fourni est invalide";

    // Domaine / service / tarif
    public static final String DOMAINE_NOM_EXISTE = "Un domaine avec ce nom existe déjà";
    public static final String SERVICE_NOM_EXISTE = "Un service avec ce nom existe déjà";
    public static final String TARIF_POSITIF = "Le tarif doit être positif ou nul";
    public static final String SERVICE_SOLO_CREATION_TRANSACTION =
            "Un service de création d'entreprise ou de transaction immobilière ne peut pas être combiné avec d'autres services";

    // Demande sans société (création d'entreprise)
    public static final String DEMANDE_SOCIETE_REQUISE =
            "La société est obligatoire sauf pour une création d'entreprise";
    public static final String RESPONSABLE_SANS_SOCIETE =
            "Un responsable ne peut pas être lié à une demande sans société";

    // Dossier de création d'entreprise ou de société
    public static final String DOSSIER_DEJA_EXISTANT =
            "Cette demande possède déjà un dossier de création";
    public static final String DOSSIER_SERVICE_NON_CREATION =
            "Un dossier de création ne peut être lié qu'à une demande de création d'entreprise ou de société";
    public static final String DOSSIER_ASSOCIES_REQUIS =
            "Une société doit avoir au moins un associé renseigné";
    public static final String DOSSIER_PRECISION_REQUISE =
            "La précision de la forme juridique est obligatoire (Autre)";
    public static final String DOSSIER_FORME_INVALIDE = "La forme juridique fournie est invalide";

    // Dossier de transaction immobilière
    public static final String DOSSIER_TRANSACTION_DEJA_EXISTANT =
            "Cette demande possède déjà un dossier de transaction immobilière";
    public static final String DOSSIER_TRANSACTION_SERVICE_NON_TRANSACTION =
            "Un dossier de transaction immobilière ne peut être lié qu'à une demande comportant un service de transaction immobilière";
    public static final String DOSSIER_TRANSACTION_ACTION_REQUISE =
            "Au moins une action doit être sélectionnée";
    public static final String DOSSIER_TRANSACTION_ACTION_AUTRE_PRECISION_REQUISE =
            "La précision de l'action est obligatoire (Autre)";
    public static final String DOSSIER_TRANSACTION_OBJET_INVALIDE =
            "L'objet fourni est invalide";
    public static final String DOSSIER_TRANSACTION_OBJET_AUTRE_PRECISION_REQUISE =
            "La précision de l'objet est obligatoire (Autre)";
    public static final String DOSSIER_TRANSACTION_TYPE_TERRAIN_INVALIDE =
            "Le type de terrain fourni est invalide";
    public static final String DOSSIER_TRANSACTION_SITUATION_TERRAIN_INVALIDE =
            "La situation du terrain fournie est invalide";

    // Demandes DEBOURS
    public static final String CATEGORIE_DEMANDE_INVALIDE =
            "La catégorie de demande doit être EXTERNALISATION ou DEBOURS";
    public static final String DESIGNATION_DEBOURS_OBLIGATOIRE =
            "La désignation du débours est obligatoire pour une demande DEBOURS";
    public static final String MONTANT_DEBOURS_POSITIF =
            "Le montant du débours doit être supérieur à 0";
    public static final String DEBOURS_MONTANT_DEJA_VALIDE =
            "Le montant du débours a déjà été validé";
    public static final String DEBOURS_MONTANT_NON_VALIDE =
            "Le montant du débours n'a pas encore été validé";
    public static final String DEBOURS_DEJA_PAYE =
            "Ce débours est déjà intégralement payé";
    public static final String DEBOURS_ECHEANCE_INTERDITE =
            "Un paiement DEBOURS ne doit pas être rattaché à une échéance";
    public static final String DEBOURS_SERVICE_INTERDIT =
            "Une demande DEBOURS ne peut pas contenir de service";
    public static final String DEBOURS_UNIQUEMENT =
            "Cette action n'est réservée qu'aux demandes DEBOURS";
    public static final String DEBOURS_STATUT_AUTOMATIQUE =
            "Le statut d'un débours est automatiquement déterminé (validation du montant puis paiements)";

    // Lignes EXTERNALISATION STANDARD : PRESTATION / ABONNEMENT_MENSUEL
    public static final String TYPE_PRESTATION_INVALIDE =
            "Le type doit être PRESTATION ou ABONNEMENT_MENSUEL";
    public static final String TYPE_PRESTATION_INTERDIT =
            "Le type de prestation ne s'applique qu'aux lignes EXTERNALISATION de service STANDARD";

    // Général
    public static final String ERREUR_INTERNE = "Une erreur interne est survenue. Veuillez réessayer.";
}
