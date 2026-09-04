package com.cabex.backend.recherche.dto;

import java.util.List;

public record RechercheGlobaleResponse(
        List<RechercheClientItem> clients,
        List<RechercheServiceItem> services,
        List<RechercheDemandeItem> demandes,
        List<RecherchePaiementItem> paiements
) {
}
