package com.cabex.backend.service.mapper;

import com.cabex.backend.service.dto.DomaineResponse;
import com.cabex.backend.service.entity.DomaineService;

public class DomaineMapper {

    private DomaineMapper() {
    }

    public static DomaineResponse toResponse(DomaineService domaine) {
        if (domaine == null) {
            return null;
        }
        return new DomaineResponse(
                domaine.getId(),
                domaine.getNomDomaine(),
                domaine.getCodeDomaine(),
                domaine.getDescription(),
                domaine.getActif()
        );
    }
}
