package com.cabex.backend.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SocieteResponse(
        Integer id,
        String nomSociete,
        String activite,
        String contact,
        String email,
        String fb,
        String adresse,
        LocalDate dateCreation,
        Integer nombrePersonnel,
        BigDecimal chiffreAffaireMensuel,
        Boolean actif,
        Integer nombreDemandes,
        LocalDateTime createdAt,
        Integer domaineId,
        String nomDomaine
) {
}
