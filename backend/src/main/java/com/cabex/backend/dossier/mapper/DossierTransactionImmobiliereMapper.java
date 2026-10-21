package com.cabex.backend.dossier.mapper;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.dossier.dto.DossierTransactionImmobiliereResponse;
import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;

public final class DossierTransactionImmobiliereMapper {

    private DossierTransactionImmobiliereMapper() {
    }

    public static DossierTransactionImmobiliereResponse toResponse(
            DossierTransactionImmobiliere dossier) {
        if (dossier == null) {
            return null;
        }
        DemandeService demande = dossier.getDemande();
        Societe societe = demande != null ? demande.getSociete() : null;
        return new DossierTransactionImmobiliereResponse(
                dossier.getId(),
                demande != null ? demande.getId() : null,
                demande != null ? demande.getReferenceDemande() : null,
                demande != null ? demande.getDateDemande() : null,
                demande != null && demande.getCommercial() != null
                        ? demande.getCommercial().getNomCommercial() : null,
                demande != null && demande.getCommercial() != null
                        ? demande.getCommercial().getImmatricule() : null,
                societe != null ? societe.getNomSociete() : null,
                societe != null ? societe.getContact() : null,
                societe != null ? societe.getEmail() : null,
                societe != null ? societe.getFb() : null,
                societe != null ? societe.getAdresse() : null,
                dossier.getNomProprietairePrecedent(),
                dossier.getActionAchat(),
                dossier.getActionVente(),
                dossier.getActionLocation(),
                dossier.getActionRegularisationPapiers(),
                dossier.getActionGestionImmobiliere(),
                dossier.getActionAutre(),
                dossier.getActionAutrePrecision(),
                dossier.getObjet(),
                dossier.getObjetAutrePrecision(),
                dossier.getAchatMesureM2(),
                dossier.getAchatPosition(),
                dossier.getAchatAxeEmplacement(),
                dossier.getAchatMontant(),
                dossier.getAchatPermisConstruire(),
                dossier.getVenteMesureM2(),
                dossier.getVentePosition(),
                dossier.getVenteAxeEmplacement(),
                dossier.getVentePrisePhoto(),
                dossier.getVenteVisite(),
                dossier.getVenteMontant(),
                dossier.getVentePermisConstruire(),
                dossier.getRegularisationTypeTerrain(),
                dossier.getRegularisationSituationTerrain(),
                dossier.getDocumentPvReconnaissance(),
                dossier.getDocumentNumeroDemandeAcquisition(),
                dossier.getDocumentActeVentePriveeCommunale(),
                dossier.getDocumentSituationJuridique(),
                dossier.getDocumentActeNotoriete(),
                dossier.getObservations(),
                dossier.getNombreCommerciauxTerrain(),
                dossier.getCommissionCn(),
                dossier.getPrestationAbonnementMensuel());
    }
}
