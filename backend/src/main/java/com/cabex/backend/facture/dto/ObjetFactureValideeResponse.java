package com.cabex.backend.facture.dto;

/**
 * Objet précis d'une facture validée tel qu'affiché sur le reçu de paiement :
 * libellé (« Nom du service », « Désignation du débours », « Services facturés »,
 * « Objet de la facture » …) et valeur résolue (services, débours ou « Facture générale »).
 */
public record ObjetFactureValideeResponse(
        String libelleObjet,
        String objet
) {
}
