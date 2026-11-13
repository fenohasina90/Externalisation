package com.cabex.backend.paiement.mapper;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.document.service.MontantEnLettres;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.paiement.dto.ModePaiementResponse;
import com.cabex.backend.paiement.dto.PaiementDetailResponse;
import com.cabex.backend.paiement.dto.PaiementResponse;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        FactureValidee fv = paiement.getFactureValidee();
        return new PaiementResponse(
                paiement.getId(),
                paiement.getReferencePaiement(),
                paiement.getDemande() != null ? paiement.getDemande().getId() : null,
                paiement.getDemande() != null ? paiement.getDemande().getReferenceDemande() : null,
                paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                        ? paiement.getDemande().getSociete().getId() : null,
                paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                        ? paiement.getDemande().getSociete().getNomSociete() : null,
                fv != null ? fv.getId() : null,
                fv != null ? fv.getReferenceFactureValidee() : null,
                paiement.getEcheance() != null ? paiement.getEcheance().getId() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getId() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getLibelle() : null,
                paiement.getDatePaiement(),
                paiement.getMontantPaye(),
                paiement.getReferenceVirement(),
                paiement.getBanque(),
                paiement.getNumeroCheque(),
                paiement.getCommentaire(),
                paiement.getPieceJustificative(),
                paiement.getCreatedBy() != null ? paiement.getCreatedBy().getId() : null,
                paiement.getCreatedBy() != null
                        ? paiement.getCreatedBy().getPrenom() + " " + paiement.getCreatedBy().getNom()
                        : null
        );
    }

    /**
     * Objet précis de la facture liée : désignation du débours (préfixée rien),
     * ou nom du service ; repliés sur le libellé générique de la ligne.
     */
    private static String objetDetailPaiement(LigneService ligne) {
        if (ligne == null) {
            return null;
        }
        if (LigneServiceService.estDebours(ligne)) {
            String d = ligne.getDesignationDebours();
            return d != null && !d.isBlank() ? d.trim() : "Débours";
        }
        String nom = ligne.getService() != null ? ligne.getService().getNomService() : null;
        return nom != null && !nom.isBlank() ? nom.trim()
                : com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(ligne);
    }

    /**
     * Détail d'un paiement. {@code sommePayeeSurFacture} est la somme des
     * paiements actifs sur la facture validée concernée (le reste à payer
     * de la facture en tient compte).
     */
    public static PaiementDetailResponse toDetailResponse(Paiement paiement, BigDecimal sommePayeeSurFacture) {
        if (paiement == null) {
            return null;
        }
        FactureValidee fv = paiement.getFactureValidee();
        LigneService ligne = fv != null && fv.getLigneService() != null
                ? fv.getLigneService()
                : fv != null && fv.getFactureProforma() != null
                        ? fv.getFactureProforma().getLignes().stream()
                                .filter(l -> l.getDemande() != null
                                        && l.getDemande().getId().equals(paiement.getDemande().getId()))
                                .findFirst().orElse(null)
                        : null;

        String typeFacture = fv == null ? null
                : fv.getMois() != null ? "ABONNEMENT_MENSUEL"
                : fv.getFactureProforma() != null ? "GENERALE"
                : ligne != null && LigneServiceService.estDebours(ligne) ? "DEBOURS"
                : "PRESTATION";
        LocalDate dateFactureValidee = fv != null && fv.getValideLe() != null
                ? fv.getValideLe().toLocalDate() : null;
        LocalDate moisFacture = fv != null ? fv.getMois() : null;

        BigDecimal montantFacture = fv != null ? fv.getMontant() : null;
        BigDecimal montantPayeSurFacture = sommePayeeSurFacture != null ? sommePayeeSurFacture : BigDecimal.ZERO;
        BigDecimal resteAPayerFacture = montantFacture != null
                ? montantFacture.subtract(montantPayeSurFacture).max(BigDecimal.ZERO)
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
                fv != null ? fv.getId() : null,
                fv != null ? fv.getReferenceFactureValidee() : null,
                objetDetailPaiement(ligne),
                paiement.getEcheance() != null ? paiement.getEcheance().getId() : null,
                paiement.getEcheance() != null ? paiement.getEcheance().getNumeroEcheance() : null,
                paiement.getEcheance() != null ? paiement.getEcheance().getDateEcheance() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getId() : null,
                paiement.getModePaiement() != null ? paiement.getModePaiement().getLibelle() : null,
                paiement.getDatePaiement(),
                paiement.getMontantPaye(),
                paiement.getReferenceVirement(),
                paiement.getBanque(),
                paiement.getNumeroCheque(),
                paiement.getCommentaire(),
                paiement.getPieceJustificative(),
                paiement.getCreatedBy() != null ? paiement.getCreatedBy().getId() : null,
                paiement.getCreatedBy() != null
                        ? paiement.getCreatedBy().getPrenom() + " " + paiement.getCreatedBy().getNom()
                        : null,
                montantFacture,
                montantPayeSurFacture,
                resteAPayerFacture,
                paiement.getMontantPaye() != null
                        ? MontantEnLettres.convertir(paiement.getMontantPaye().longValue()) : null,
                typeFacture,
                dateFactureValidee,
                moisFacture
        );
    }
}
