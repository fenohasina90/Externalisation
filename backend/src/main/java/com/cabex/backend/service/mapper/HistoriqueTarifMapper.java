package com.cabex.backend.service.mapper;

import com.cabex.backend.service.dto.HistoriqueTarifResponse;
import com.cabex.backend.service.entity.HistoriqueTarifService;

public class HistoriqueTarifMapper {

    private HistoriqueTarifMapper() {
    }

    public static HistoriqueTarifResponse toResponse(HistoriqueTarifService historique) {
        if (historique == null) {
            return null;
        }
        return new HistoriqueTarifResponse(
                historique.getId(),
                historique.getMontant(),
                historique.getDateDebut(),
                historique.getDateFin(),
                historique.getUtilisateur() != null ? historique.getUtilisateur().getId() : null,
                historique.getUtilisateur() != null
                        ? historique.getUtilisateur().getPrenom() + " " + historique.getUtilisateur().getNom()
                        : null
        );
    }
}
