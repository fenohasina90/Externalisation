package com.cabex.backend.echeance.mapper;

import com.cabex.backend.echeance.dto.EcheanceResponse;
import com.cabex.backend.echeance.dto.PlanPaiementResponse;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EcheanceMapper {

    private EcheanceMapper() {
    }

    public static String calculerStatutEcheance(EcheancePaiement echeance, LocalDate aujourdhui, int joursAlerte) {
        BigDecimal reste = echeance.getMontantAttendu().subtract(echeance.getMontantPaye());
        if (reste.compareTo(BigDecimal.ZERO) <= 0) {
            return "PAYEE";
        }
        if (echeance.getDateEcheance().isBefore(aujourdhui)) {
            return "EN_RETARD";
        }
        if (!echeance.getDateEcheance().isAfter(aujourdhui.plusDays(joursAlerte))) {
            return "PROCHE";
        }
        return "A_VENIR";
    }

    public static EcheanceResponse toEcheanceResponse(EcheancePaiement echeance, LocalDate aujourdhui, int joursAlerte) {
        if (echeance == null) {
            return null;
        }
        BigDecimal reste = echeance.getMontantAttendu().subtract(echeance.getMontantPaye());
        return new EcheanceResponse(
                echeance.getId(),
                echeance.getProposPaiement() != null ? echeance.getProposPaiement().getId() : null,
                echeance.getNumeroEcheance(),
                echeance.getDateEcheance(),
                echeance.getMontantAttendu(),
                echeance.getMontantPaye(),
                reste,
                calculerStatutEcheance(echeance, aujourdhui, joursAlerte),
                echeance.getCommentaire()
        );
    }

    public static PlanPaiementResponse toPlanResponse(ProposPaiement propos, LocalDate aujourdhui, int joursAlerte) {
        if (propos == null) {
            return null;
        }
        List<EcheanceResponse> echeances = propos.getEcheances() == null
                ? List.of()
                : propos.getEcheances().stream()
                        .sorted(Comparator.comparing(EcheancePaiement::getNumeroEcheance))
                        .map(e -> toEcheanceResponse(e, aujourdhui, joursAlerte))
                        .collect(Collectors.toList());
        return new PlanPaiementResponse(
                propos.getId(),
                propos.getDemande() != null ? propos.getDemande().getId() : null,
                propos.getDemande() != null ? propos.getDemande().getReferenceDemande() : null,
                propos.getDateDebut(),
                propos.getDateFin(),
                propos.getMontantTotal(),
                propos.getRestePayer(),
                propos.getNombreEcheances(),
                propos.getCommentaire(),
                propos.getStatut(),
                echeances
        );
    }
}
