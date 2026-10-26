package com.cabex.backend.dossier.mapper;

import com.cabex.backend.dossier.dto.AssocieResponse;
import com.cabex.backend.dossier.dto.DossierResponse;
import com.cabex.backend.dossier.entity.DossierAssocie;
import com.cabex.backend.dossier.entity.DossierCreation;

import java.util.Comparator;
import java.util.List;

public class DossierMapper {

    private DossierMapper() {
    }

    public static AssocieResponse toResponse(DossierAssocie associe) {
        if (associe == null) {
            return null;
        }
        return new AssocieResponse(
                associe.getId(),
                associe.getOrdre(),
                associe.getNom(),
                associe.getPartSociale(),
                associe.getEmail(),
                associe.getContact());
    }

    public static DossierResponse toResponse(DossierCreation dossier) {
        if (dossier == null) {
            return null;
        }
        List<AssocieResponse> associes = dossier.getAssocies() == null ? List.of()
                : dossier.getAssocies().stream()
                        .sorted(Comparator.comparing(
                                DossierAssocie::getOrdre,
                                Comparator.nullsLast(Integer::compareTo)))
                        .map(DossierMapper::toResponse)
                        .toList();
        String numeroIm = null;
        if (dossier.getDemande() != null && dossier.getDemande().getCommercial() != null) {
            numeroIm = dossier.getDemande().getCommercial().getImmatricule();
        }
        return new DossierResponse(
                dossier.getId(),
                dossier.getDemande() != null ? dossier.getDemande().getId() : null,
                dossier.getDemande() != null ? dossier.getDemande().getReferenceDemande() : null,
                dossier.getDemande() != null ? dossier.getDemande().getDateDemande() : null,
                numeroIm,
                dossier.getFormeJuridique(),
                dossier.getFormeJuridiquePrecision(),
                dossier.getDenomination(),
                dossier.getSiegeSocialActuel(),
                dossier.getCapitalSocial(),
                dossier.getNombreAssociesDeclare(),
                dossier.getNomGerant(),
                dossier.getFonctionGerant(),
                dossier.getLocationGerance(),
                dossier.getSecteurActivite(),
                dossier.getAutorisationSpeciale(),
                dossier.getAdresseSiege(),
                dossier.getVille(),
                dossier.getCommune(),
                dossier.getDistrict(),
                dossier.getRegion(),
                dossier.getTypeHebergement(),
                dossier.getHebergementPrecision(),
                dossier.getContratBail(),
                dossier.getContratBailDetail(),
                dossier.getObjetSocial(),
                dossier.getDateSouhaiteeDebut(),
                dossier.getRegimeActivite(),
                dossier.getActivitesEtablissement(),
                dossier.getObservations(),
                dossier.getAssujettissementTva(),
                dossier.getAutreSociete(),
                dossier.getSituationMatrimoniale(),
                dossier.getRegimeImposition(),
                associes);
    }
}
