package com.cabex.backend.demande.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LigneServiceResponse(
        Integer id,
        Integer demandeId,
        String referenceDemande,
        Integer serviceId,
        String nomService,
        String typeService,
        BigDecimal tarif,
        LocalDate debutService,
        LocalDate finService,
        String statut,
        Integer factureId,
        String referenceFacture,
        BigDecimal montantPaye,
        BigDecimal resteAPayer
) {
}
