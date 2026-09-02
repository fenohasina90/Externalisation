package com.cabex.backend.service.mapper;

import com.cabex.backend.service.dto.DomaineResponse;
import com.cabex.backend.service.entity.DomaineService;

public class DomaineMapper {

    private DomaineMapper() {
    }

    public static DomaineResponse toResponse(DomaineService domaine, long nombreServices) {
        if (domaine == null) {
            return null;
        }
        return new DomaineResponse(
                domaine.getId(),
                domaine.getNomDomaine(),
                domaine.getDescription(),
                domaine.getActif(),
                nombreServices
        );
    }

    public static DomaineResponse toResponse(DomaineService domaine) {
        return toResponse(domaine, domaine != null && domaine.getServices() != null
                ? domaine.getServices().size()
                : 0);
    }
}
