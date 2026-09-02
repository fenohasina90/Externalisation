package com.cabex.backend.service.mapper;

import com.cabex.backend.service.dto.ServiceResponse;
import com.cabex.backend.service.entity.Service;

public class ServiceMapper {

    private ServiceMapper() {
    }

    public static ServiceResponse toResponse(Service service) {
        if (service == null) {
            return null;
        }
        return new ServiceResponse(
                service.getId(),
                service.getNomService(),
                service.getTarifService(),
                service.getDescription(),
                service.getDomaine() != null ? service.getDomaine().getId() : null,
                service.getDomaine() != null ? service.getDomaine().getNomDomaine() : null,
                service.getActif()
        );
    }
}
