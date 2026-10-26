package com.cabex.backend.paiement.mapper;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.document.service.MontantEnLettres;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.paiement.dto.ModePaiementResponse;
import com.cabex.backend.paiement.dto.PaiementDetailResponse;
import com.cabex.backend.paiement.dto.PaiementResponse;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;

import java.math.BigDecimal;

public class PaiementMapper {

    private PaiementMapper() {
    }

    public static ModePaiementResponse toModeResponse(ModePaiement mode) {
        if (mode == null) {
            return null;
        }
        return new ModePaiementResponse(
                mode.getId(),
                mode.getCode(),
                mode.getLibelle(),
                mode.getActif()
        );
    }

    public static PaiementResponse toResponse(Paiement paiement) {
        if (paiement == null) {
            return null;
        }
        return new PaiementResponse(
                paiement.getId(),
                paiement.getReferencePaiement(),
                paiement.getDemande() != null ? paiement.getDemande().getId() : null,
                paiement.getDemande() != null ? paiement.getDemande().getReferenceDemande() : null,
                paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                        ? paiement.getDemande().getSociete().getId() : null,
                paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                        ? paiement.getDemande().getSociete().getNomSociete() : null,
                paiement.getEcheance() != null ? paiement.getEcheance().getId() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getId() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getLibelle() : null,
                paiement.getDatePaiement(),
                paiement.getMontantPaye(),
                paiement.getReferenceVirement(),
                paiement.getCommentaire(),
                paiement.getPieceJustificative(),
                paiement.getCreatedBy() != null ? paiement.getCreatedBy().getId() : null,
                paiement.getCreatedBy() != null
                        ? paiement.getCreatedBy().getPrenom() + " " + paiement.getCreatedBy().getNom()
                        : null
        );
    }

    public static PaiementDetailResponse toDetailResponse(Paiement paiement) {
        if (paiement == null) {
            return null;
        }
        EcheancePaiement echeance = paiement.getEcheance();
        LigneService ligne = echeance != null && echeance.getProposPaiement() != null
                ? echeance.getProposPaiement().getLigneService() : null;

        BigDecimal montantAttendu = echeance != null ? echeance.getMontantAttendu() : null;
        BigDecimal montantPayeEcheance = echeance != null
                ? echeance.getMontantPaye() : paiement.getMontantPaye();
        BigDecimal resteAPayer = montantAttendu != null
                ? montantAttendu.subtract(montantPayeEcheance != null
                        ? montantPayeEcheance : BigDecimal.ZERO)
                : null;

        return new PaiementDetailResponse(
                paiement.getId(),
                paiement.getReferencePaiement(),
                paiement.getDemande() != null ? paiement.getDemande().getId() : null,
                paiement.getDemande() != null ? paiement.getDemande().getReferenceDemande() : null,
                paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                        ? paiement.getDemande().getSociete().getId() : null,
                paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                        ? paiement.getDemande().getSociete().getNomSociete() : null,
                echeance != null ? echeance.getId() : null,
                echeance != null ? echeance.getNumeroEcheance() : null,
                echeance != null ? echeance.getDateEcheance() : null,
                ligne != null && ligne.getService() != null
                        ? ligne.getService().getNomService() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getId() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getLibelle() : null,
                paiement.getDatePaiement(),
                paiement.getMontantPaye(),
                paiement.getReferenceVirement(),
                paiement.getCommentaire(),
                paiement.getPieceJustificative(),
                paiement.getCreatedBy() != null ? paiement.getCreatedBy().getId() : null,
                paiement.getCreatedBy() != null
                        ? paiement.getCreatedBy().getPrenom() + " " + paiement.getCreatedBy().getNom()
                        : null,
                montantAttendu,
                montantPayeEcheance,
                resteAPayer,
                paiement.getMontantPaye() != null
                        ? MontantEnLettres.convertir(paiement.getMontantPaye().longValue()) : null
        );
    }
}
