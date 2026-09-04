package com.cabex.backend.demande.mapper;

import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.entity.StatutDemande;

public class StatutMapper {

    private StatutMapper() {
    }

    public static StatutResponse toResponse(StatutDemande statut) {
        if (statut == null) {
            return null;
        }
        return new StatutResponse(
                statut.getId(),
                statut.getStatut(),
                statut.getDescription(),
                statut.getOrdre(),
                statut.getActif()
        );
    }
}
