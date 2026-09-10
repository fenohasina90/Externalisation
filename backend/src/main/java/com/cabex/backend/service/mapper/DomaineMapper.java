package com.cabex.backend.service.mapper;

import com.cabex.backend.service.dto.DomaineResponse;
import com.cabex.backend.service.entity.DomaineService;

public class DomaineMapper {

    private DomaineMapper() {
    }

    public static DomaineResponse toResponse(DomaineService domaine, long nombreSocietes) {
        if (domaine == null) {
            return null;
        }
        return new DomaineResponse(
                domaine.getId(),
                domaine.getNomDomaine(),
                domaine.getDescription(),
                domaine.getActif(),
                nombreSocietes
        );
    }

    public static DomaineResponse toResponse(DomaineService domaine) {
        return toResponse(domaine, domaine != null && domaine.getSocietes() != null
                ? domaine.getSocietes().size()
                : 0);
    }
}
