package com.cabex.backend.demande.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DemandeResponse(
        Integer id,
        String referenceDemande,
        LocalDate dateDemande,
        LocalDate debutService,
        LocalDate finService,
        Integer societeId,
        String nomSociete,
        Integer commercialId,
        String nomCommercial,
        Integer clientResponsableId,
        String nomResponsable,
        Integer serviceId,
        String nomService,
        Integer domaineId,
        String nomDomaine,
        BigDecimal tarif,
        String commentaire,
        String statutActuel,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        Boolean actif,
        LocalDateTime createdAt,
        List<LigneServiceResponse> lignes,
        BigDecimal montantTotal,
        String situationPaiement,
        Long nombreEcheances
) {
}
