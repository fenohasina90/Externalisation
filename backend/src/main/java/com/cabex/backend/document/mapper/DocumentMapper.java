package com.cabex.backend.document.mapper;

import com.cabex.backend.document.dto.DocumentResponse;
import com.cabex.backend.document.entity.PieceJointe;

public class DocumentMapper {

    private DocumentMapper() {
    }

    public static DocumentResponse toResponse(PieceJointe piece) {
        if (piece == null) {
            return null;
        }
        return new DocumentResponse(
                piece.getId(),
                piece.getDemande() != null ? piece.getDemande().getId() : null,
                piece.getPaiement() != null ? piece.getPaiement().getId() : null,
                piece.getPaiement() != null ? piece.getPaiement().getReferencePaiement() : null,
                piece.getNomFichier(),
                piece.getNomOriginal(),
                piece.getTypeFichier(),
                piece.getTailleFichier(),
                piece.getDescription(),
                piece.getUploadedBy() != null ? piece.getUploadedBy().getId() : null,
                piece.getUploadedBy() != null
                        ? piece.getUploadedBy().getPrenom() + " " + piece.getUploadedBy().getNom()
                        : null,
                piece.getCreatedAt()
        );
    }
}
