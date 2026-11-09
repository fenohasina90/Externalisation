package com.cabex.backend.demande.mapper;

import com.cabex.backend.demande.dto.DemandeResponse;
import com.cabex.backend.demande.dto.HistoriqueDemandeResponse;
import com.cabex.backend.demande.dto.HistoriqueLigneResponse;
import com.cabex.backend.demande.dto.LigneServiceResponse;
import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.service.SituationPaiement;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.HistoriqueLigneService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;

import java.math.BigDecimal;
import java.util.List;

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
        return toResponse(d, statutActuel, montantPaye, resteAPayer, List.of(), null);
    }

    public static DemandeResponse toResponse(DemandeService d, String statutActuel,
                                             BigDecimal montantPaye, BigDecimal resteAPayer,
                                             List<LigneServiceResponse> lignes, BigDecimal montantTotal) {
        return toResponse(d, statutActuel, montantPaye, resteAPayer, lignes, montantTotal,
                SituationPaiement.NON_PLANIFIE, 0L);
    }

    public static DemandeResponse toResponse(DemandeService d, String statutActuel,
                                             BigDecimal montantPaye, BigDecimal resteAPayer,
                                             List<LigneServiceResponse> lignes, BigDecimal montantTotal,
                                             String situationPaiement, Long nombreEcheances) {
        return toResponse(d, statutActuel, montantPaye, resteAPayer, lignes, montantTotal,
                situationPaiement, nombreEcheances, null);
    }

    /**
     * Variante utilisée par les listes : les lignes sont déjà chargées en une
     * seule requête pour toute la page, ce qui évite un accès LAZY par demande.
     */
    public static DemandeResponse toResponse(DemandeService d, String statutActuel,
                                             BigDecimal montantPaye, BigDecimal resteAPayer,
                                             List<LigneServiceResponse> lignes, BigDecimal montantTotal,
                                             String situationPaiement, Long nombreEcheances,
                                             List<LigneService> entitesChargees) {
        if (d == null) {
            return null;
        }
        // Compatibilité : les champs historiques mono-service sont renseignés
        // depuis la première ligne (les clients API utilisent « lignes »).
        List<LigneService> source = entitesChargees;
        if (source == null) {
            source = d.getLignes() != null ? List.copyOf(d.getLignes()) : List.of();
        }
        List<LigneService> entites = source.stream().sorted(
                        (a, b) -> Integer.compare(
                                a.getId() != null ? a.getId() : 0,
                                b.getId() != null ? b.getId() : 0)).toList();
        LigneService ref = entites.isEmpty() ? null : entites.get(0);
        return new DemandeResponse(
                d.getId(),
                d.getReferenceDemande(),
                d.getDateDemande(),
                ref != null ? ref.getDebutService() : null,
                ref != null ? ref.getFinService() : null,
                d.getSociete() != null ? d.getSociete().getId() : null,
                d.getSociete() != null ? d.getSociete().getNomSociete() : null,
                d.getCommercial() != null ? d.getCommercial().getId() : null,
                d.getCommercial() != null ? d.getCommercial().getNomCommercial() : null,
                d.getClientResponsable() != null ? d.getClientResponsable().getId() : null,
                d.getClientResponsable() != null ? d.getClientResponsable().getNomClient() : null,
                ref != null && ref.getService() != null ? ref.getService().getId() : null,
                ref != null && ref.getService() != null ? ref.getService().getNomService() : null,
                ref != null && ref.getService() != null && ref.getService().getDomaine() != null
                        ? ref.getService().getDomaine().getId() : null,
                ref != null && ref.getService() != null && ref.getService().getDomaine() != null
                        ? ref.getService().getDomaine().getNomDomaine() : null,
                ref != null ? ref.getTarif() : null,
                d.getCommentaire(),
                statutActuel,
                montantPaye,
                resteAPayer,
                d.getActif(),
                d.getCreatedAt(),
                lignes,
                montantTotal,
                situationPaiement,
                nombreEcheances
        );
    }

    public static LigneServiceResponse toLigneResponse(LigneService l, BigDecimal montantPaye,
                                                       BigDecimal resteAPayer) {
        if (l == null) {
            return null;
        }
        return new LigneServiceResponse(
                l.getId(),
                l.getDemande() != null ? l.getDemande().getId() : null,
                l.getDemande() != null ? l.getDemande().getReferenceDemande() : null,
                l.getService() != null ? l.getService().getId() : null,
                l.getService() != null ? l.getService().getNomService() : null,
                l.getService() != null ? l.getService().getTypeService() : null,
                l.getTarif(),
                l.getDebutService(),
                l.getFinService(),
                l.getStatut(),
                l.getFacture() != null ? l.getFacture().getId() : null,
                l.getFacture() != null ? l.getFacture().getReferenceFacture() : null,
                montantPaye,
                resteAPayer
        );
    }

    public static HistoriqueLigneResponse toHistoriqueLigneResponse(HistoriqueLigneService h,
                                                                    String ancienStatut) {
        if (h == null) {
            return null;
        }
        return new HistoriqueLigneResponse(
                h.getId(),
                h.getLigneService() != null ? h.getLigneService().getId() : null,
                h.getStatut(),
                ancienStatut,
                h.getCommentaire(),
                h.getUtilisateur() != null ? h.getUtilisateur().getId() : null,
                h.getUtilisateur() != null
                        ? h.getUtilisateur().getPrenom() + " " + h.getUtilisateur().getNom()
                        : null,
                h.getOrigine(),
                h.getCreatedAt()
        );
    }
}
