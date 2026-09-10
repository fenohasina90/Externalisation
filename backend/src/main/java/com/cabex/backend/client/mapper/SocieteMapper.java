package com.cabex.backend.client.mapper;

import com.cabex.backend.client.dto.SocieteResponse;
import com.cabex.backend.client.entity.Societe;

public class SocieteMapper {

    private SocieteMapper() {
    }

    public static SocieteResponse toResponse(Societe societe, Integer nombreDemandes) {
        if (societe == null) {
            return null;
        }
        return new SocieteResponse(
                societe.getId(),
                societe.getNomSociete(),
                societe.getActivite(),
                societe.getContact(),
                societe.getEmail(),
                societe.getFb(),
                societe.getAdresse(),
                societe.getDateCreation(),
                societe.getNombrePersonnel(),
                societe.getChiffreAffaireMensuel(),
                societe.getActif(),
                nombreDemandes,
                societe.getCreatedAt(),
                societe.getDomaine() != null ? societe.getDomaine().getId() : null,
                societe.getDomaine() != null ? societe.getDomaine().getNomDomaine() : null
        );
    }

    public static SocieteResponse toResponse(Societe societe) {
        return toResponse(societe, societe != null && societe.getDemandes() != null
                ? societe.getDemandes().size()
                : 0);
    }
}
