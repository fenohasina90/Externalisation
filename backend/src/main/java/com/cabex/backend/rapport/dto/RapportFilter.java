package com.cabex.backend.rapport.dto;

import java.time.LocalDate;

public record RapportFilter(
        LocalDate dateDebut,
        LocalDate dateFin,
        Integer clientId,
        Integer serviceId,
        Integer commercialId,
        String statut
) {

    public static RapportFilter of(LocalDate dateDebut, LocalDate dateFin, Integer clientId,
                                   Integer serviceId, Integer commercialId,
                                   String statut) {
        return new RapportFilter(dateDebut, dateFin, clientId, serviceId, commercialId, statut);
    }
}
