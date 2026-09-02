package com.cabex.backend.commercial.mapper;

import com.cabex.backend.commercial.dto.CommercialResponse;
import com.cabex.backend.commercial.entity.Commercial;

public class CommercialMapper {

    private CommercialMapper() {
    }

    public static CommercialResponse toResponse(Commercial commercial) {
        if (commercial == null) {
            return null;
        }
        return new CommercialResponse(
                commercial.getId(),
                commercial.getNomCommercial(),
                commercial.getImmatricule(),
                commercial.getEmail(),
                commercial.getContact(),
                commercial.getActif()
        );
    }
}
