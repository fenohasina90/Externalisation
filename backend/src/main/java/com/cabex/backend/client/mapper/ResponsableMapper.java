package com.cabex.backend.client.mapper;

import com.cabex.backend.client.dto.ResponsableResponse;
import com.cabex.backend.client.entity.ClientResponsable;

public class ResponsableMapper {

    private ResponsableMapper() {
    }

    public static ResponsableResponse toResponse(ClientResponsable responsable) {
        if (responsable == null) {
            return null;
        }
        return new ResponsableResponse(
                responsable.getId(),
                responsable.getSociete() != null ? responsable.getSociete().getId() : null,
                responsable.getSociete() != null ? responsable.getSociete().getNomSociete() : null,
                responsable.getNomClient(),
                responsable.getContact(),
                responsable.getEmail(),
                responsable.getFonction(),
                responsable.getActif()
        );
    }
}
