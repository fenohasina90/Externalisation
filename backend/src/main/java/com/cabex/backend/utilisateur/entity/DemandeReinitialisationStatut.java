package com.cabex.backend.utilisateur.entity;

/**
 * Statuts d'une demande de réinitialisation de mot de passe :
 * <ul>
 *   <li>{@code ENVOYEE} : mot de passe temporaire généré et envoyé par e-mail à
 *       l'utilisateur (statut produit par le nouveau flux automatique) ;</li>
 *   <li>{@code TERMINEE} : l'utilisateur a remplacé le mot de passe temporaire ;</li>
 *   <li>{@code EXPIREE} : mot de passe temporaire non changé avant son échéance ;</li>
 *   <li>{@code EN_ATTENTE}, {@code APPROUVEE}, {@code REFUSEE} : statuts de
 *       l'ancien flux avec validation administrative. Ils ne sont plus produits
 *       mais restent lisibles pour les demandes historiques.</li>
 * </ul>
 */
public final class DemandeReinitialisationStatut {

    public static final String ENVOYEE = "ENVOYEE";
    public static final String TERMINEE = "TERMINEE";
    public static final String EXPIREE = "EXPIREE";

    /** Ancien flux (historique) — plus jamais généré. */
    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String APPROUVEE = "APPROUVEE";
    public static final String REFUSEE = "REFUSEE";

    private DemandeReinitialisationStatut() {
    }
}
