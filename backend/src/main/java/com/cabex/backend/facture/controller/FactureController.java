package com.cabex.backend.facture.controller;

import com.cabex.backend.facture.dto.CreerFactureAbonnementRequest;
import com.cabex.backend.facture.dto.FactureResponse;
import com.cabex.backend.facture.dto.FactureValideeResponse;
import com.cabex.backend.facture.dto.GenererFacturesRequest;
import com.cabex.backend.facture.dto.InclusionDisponibleResponse;
import com.cabex.backend.facture.dto.ValiderFactureRequest;
import com.cabex.backend.facture.service.FactureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Factures", description = "Factures proforma et validation")
@SecurityRequirement(name = "bearerAuth")
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @GetMapping("/demandes/{demandeId}/factures")
    @PreAuthorize("hasAuthority('FACTURE_VIEW')")
    @Operation(summary = "Lister les factures d'une demande")
    public ResponseEntity<List<FactureResponse>> listerParDemande(@PathVariable Integer demandeId) {
        return ResponseEntity.ok(factureService.listerParDemande(demandeId));
    }

    @GetMapping("/factures/{id}")
    @PreAuthorize("hasAuthority('FACTURE_VIEW')")
    @Operation(summary = "Consulter une facture")
    public ResponseEntity<FactureResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(factureService.getById(id));
    }

    @PostMapping("/demandes/{demandeId}/factures")
    @PreAuthorize("hasAuthority('FACTURE_CREATE')")
    @Operation(summary = "Générer les factures proforma d'une demande EN_ATTENTE")
    public ResponseEntity<List<FactureResponse>> generer(@PathVariable Integer demandeId,
                                                         @Valid @RequestBody GenererFacturesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(factureService.genererPourDemande(demandeId, request.modeFacturation()));
    }

    @GetMapping("/demandes/{demandeId}/factures-validees")
    @PreAuthorize("hasAuthority('FACTURE_VIEW')")
    @Operation(summary = "Lister les factures validées d'une demande (suivi paiements)")
    public ResponseEntity<List<FactureValideeResponse>> listerValidees(@PathVariable Integer demandeId) {
        return ResponseEntity.ok(factureService.listerFacturesValidees(demandeId));
    }

    @PostMapping("/demandes/{demandeId}/factures-abonnement")
    @PreAuthorize("hasAuthority('FACTURE_ABONNEMENT_CREATE')")
    @Operation(summary = "Créer la facture du mois d'un abonnement (enfant de la facture générale validée)",
            description = "Montant = tarif mensuel + éléments inclus (prestation / débours impayés "
                    + "de la même société, en clair dans la facture). La facture affiche les références "
                    + ": facture générale validée, facture proforma et demande. "
                    + "La date d'émission de la facture est saisie par l'utilisateur.")
    public ResponseEntity<FactureValideeResponse> creerFactureAbonnement(
            @PathVariable Integer demandeId,
            @Valid @RequestBody CreerFactureAbonnementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(factureService.creerFactureMensuelleAbonnement(
                        demandeId, request.ligneServiceId(), request.dateEmission(),
                        request.mois(), request.commentaire(), request.inclusionLigneIds()));
    }

    @GetMapping("/demandes/{demandeId}/factures-abonnement/inclusions-disponibles")
    @PreAuthorize("hasAuthority('FACTURE_ABONNEMENT_CREATE')")
    @Operation(summary = "Éléments impayés pouvant être repris dans la facture du mois",
            description = "Lignes prestation ou débours non soldées des demandes EN_COURS de la même société (client).")
    public ResponseEntity<List<InclusionDisponibleResponse>> inclusionsDisponibles(
            @PathVariable Integer demandeId) {
        return ResponseEntity.ok(factureService.inclusionsDisponibles(demandeId));
    }

    @PutMapping("/factures/{id}/valider")
    @PreAuthorize("hasAuthority('FACTURE_VALIDATE')")
    @Operation(summary = "Valider une facture proforma",
            description = "Crée la facture validée (nouvelle référence XXXXXEXT/TNR/YYYY) avec un montant propre, qui peut différer du montant proforma")
    public ResponseEntity<FactureResponse> valider(@PathVariable Integer id,
                                                   @RequestBody(required = false) ValiderFactureRequest request) {
        return ResponseEntity.ok(factureService.valider(
                id,
                request != null ? request.commentaire() : null,
                request != null ? request.montant() : null,
                request != null ? request.lignes() : null));
    }
}
