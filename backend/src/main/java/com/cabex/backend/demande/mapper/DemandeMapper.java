package com.cabex.backend.demande.mapper;

import com.cabex.backend.demande.dto.DemandeResponse;
import com.cabex.backend.demande.dto.HistoriqueDemandeResponse;
import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.StatutDemande;

import java.math.BigDecimal;

public class DemandeMapper {

    private DemandeMapper() {
    }

    public static StatutResponse toStatutResponse(StatutDemande statut) {
        if (statut == null) {
            return null;
        }
        return new StatutResponse(
                statut.getId(),
                statut.getStatut(),
                statut.getDescription(),
                statut.getOrdre(),
                statut.getActif()
        );
    }

    public static HistoriqueDemandeResponse toHistoriqueResponse(HistoriqueDemande h) {
        return toHistoriqueResponse(h, null);
    }

    public static HistoriqueDemandeResponse toHistoriqueResponse(HistoriqueDemande h, String ancienStatut) {
        if (h == null) {
            return null;
        }
        return new HistoriqueDemandeResponse(
                h.getId(),
                h.getStatut() != null ? h.getStatut().getId() : null,
                h.getStatut() != null ? h.getStatut().getStatut() : null,
                ancienStatut,
                h.getCommentaire(),
                h.getUtilisateur() != null ? h.getUtilisateur().getId() : null,
                h.getUtilisateur() != null
                        ? h.getUtilisateur().getPrenom() + " " + h.getUtilisateur().getNom()
                        : null,
                h.getCreatedAt()
        );
    }

    public static DemandeResponse toResponse(DemandeService d, String statutActuel,
                                             BigDecimal montantPaye, BigDecimal resteAPayer) {
        if (d == null) {
            return null;
        }
        return new DemandeResponse(
                d.getId(),
                d.getReferenceDemande(),
                d.getDateDemande(),
                d.getDebutService(),
                d.getFinService(),
                d.getSociete() != null ? d.getSociete().getId() : null,
                d.getSociete() != null ? d.getSociete().getNomSociete() : null,
                d.getCommercial() != null ? d.getCommercial().getId() : null,
                d.getCommercial() != null ? d.getCommercial().getNomCommercial() : null,
                d.getClientResponsable() != null ? d.getClientResponsable().getId() : null,
                d.getClientResponsable() != null ? d.getClientResponsable().getNomClient() : null,
                d.getService() != null ? d.getService().getId() : null,
                d.getService() != null ? d.getService().getNomService() : null,
                d.getSociete() != null && d.getSociete().getDomaine() != null
                        ? d.getSociete().getDomaine().getId() : null,
                d.getSociete() != null && d.getSociete().getDomaine() != null
                        ? d.getSociete().getDomaine().getNomDomaine() : null,
                d.getTarif(),
                d.getCommentaire(),
                statutActuel,
                montantPaye,
                resteAPayer,
                d.getActif(),
                d.getCreatedAt()
        );
    }
}
