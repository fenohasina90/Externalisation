package com.cabex.backend.client.dto;

import java.util.List;

public record SocieteDetailResponse(
        SocieteResponse societe,
        List<ResponsableResponse> responsables,
        List<DemandeFicheResponse> demandes,
        List<ServiceUtiliseResponse> servicesUtilises,
        List<PaiementFicheResponse> paiements,
        List<EcheanceFicheResponse> echeances
) {
}
