package com.cabex.backend.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoriqueTarifResponse(
        Integer id,
        BigDecimal montant,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Integer utilisateurId,
        String nomUtilisateur
) {
}
