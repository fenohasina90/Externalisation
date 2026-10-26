package com.cabex.backend.utilisateur.entity;

/**
 * Statuts d'une demande de réinitialisation de mot de passe (flux « mot de
 * passe temporaire ») :
 * <ul>
 *   <li>{@code EN_ATTENTE} : demande créée par l'utilisateur, en attente d'approbation ;</li>
 *   <li>{@code APPROUVEE}  : demande approuvée par un administrateur, éventuellement
 *       après quoi un mot de passe temporaire a été généré ;</li>
 *     <li>{@code REFUSEE}    : demande refusée par un administrateur ;</li>
 *     <li>{@code EXPIREE}    : demande non traitée à temps ou mot de passe temporaire expire ;</li>
 *     <li>{@code TERMINEE}</li>
 * </ul>
 */
public final class DemandeReinitialisationStatut {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String APPROUVEE = "APPROUVEE";
    public static final String REFUSEE = "REFUSEE";
    public static final String EXPIREE = "EXPIREE";
    public static final String TERMINEE = "TERMINEE";

    private DemandeReinitialisationStatut() {
    }
}