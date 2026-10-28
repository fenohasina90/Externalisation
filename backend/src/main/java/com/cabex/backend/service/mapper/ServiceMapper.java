package com.cabex.backend.service.mapper;

import com.cabex.backend.service.dto.ServiceResponse;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.entity.Service;

public class ServiceMapper {

    private ServiceMapper() {
    }

    public static ServiceResponse toResponse(Service service) {
        if (service == null) {
            return null;
        }
        DomaineService domaine = service.getDomaine();
        return new ServiceResponse(
                service.getId(),
                service.getNomService(),
                service.getCodeService(),
                service.getTypeService(),
                service.getDescription(),
                service.getActif(),
                domaine != null ? domaine.getId() : null,
                domaine != null ? domaine.getCodeDomaine() : null
        );
    }
}
