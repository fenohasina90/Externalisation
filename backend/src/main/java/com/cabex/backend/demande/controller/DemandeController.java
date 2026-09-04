package com.cabex.backend.demande.controller;

import com.cabex.backend.demande.dto.ChangeStatutRequest;
import com.cabex.backend.demande.dto.CreateDemandeRequest;
import com.cabex.backend.demande.dto.DemandeResponse;
import com.cabex.backend.demande.dto.HistoriqueDemandeResponse;
import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.dto.UpdateDemandeRequest;
import com.cabex.backend.demande.service.DemandeGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demandes")
@Tag(name = "Demandes", description = "Gestion des demandes de service")
@SecurityRequirement(name = "bearerAuth")
public class DemandeController {

    private final DemandeGestionService demandeService;

    public DemandeController(DemandeGestionService demandeService) {
        this.demandeService = demandeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Lister / rechercher / filtrer les demandes",
            description = "Pagination, recherche par référence, filtres société/service/commercial/statut/dates/actif")
    public ResponseEntity<Page<DemandeResponse>> getAll(
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Integer societeId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer domaineId,
            @RequestParam(required = false) Integer commercialId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Boolean actif,
            Pageable pageable) {
        return ResponseEntity.ok(demandeService.getAll(recherche, societeId, serviceId,
                domaineId, commercialId, statut, dateDebut, dateFin, actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Consulter le détail d'une demande",
            description = "Informations client, service, dates, tarif, statut, montant payé / restant")
    public ResponseEntity<DemandeResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(demandeService.getById(id));
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Consulter l'historique / timeline d'une demande",
            description = "Chaque changement de statut enregistre utilisateur, ancien/nouveau statut, commentaire et date")
    public ResponseEntity<List<HistoriqueDemandeResponse>> getHistorique(@PathVariable Integer id) {
        return ResponseEntity.ok(demandeService.getHistorique(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEMANDE_CREATE')")
    @Operation(summary = "Créer une demande de service",
            description = "Génère une référence unique DEM-YYYY-NNNNN et crée l'historique initial (EN_ATTENTE)")
    public ResponseEntity<DemandeResponse> create(@Valid @RequestBody CreateDemandeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(demandeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Modifier une demande", description = "Autorisé tant que la demande n'est pas TERMINEE ou ANNULEE")
    public ResponseEntity<DemandeResponse> update(@PathVariable Integer id,
                                                  @Valid @RequestBody UpdateDemandeRequest request) {
        return ResponseEntity.ok(demandeService.update(id, request));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('DEMANDE_STATUS_UPDATE')")
    @Operation(summary = "Changer le statut d'une demande",
            description = "Cycle EN_ATTENTE → APPROUVEE → EN_COURS → TERMINEE, ou → ANNULEE ; chaque changement est historié")
    public ResponseEntity<DemandeResponse> changeStatut(@PathVariable Integer id,
                                                        @Valid @RequestBody ChangeStatutRequest request) {
        return ResponseEntity.ok(demandeService.changeStatut(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Archiver / désarchiver une demande")
    public ResponseEntity<DemandeResponse> archive(@PathVariable Integer id,
                                                   @RequestBody Map<String, Boolean> body) {
        boolean archive = Boolean.TRUE.equals(body.get("archive"));
        return ResponseEntity.ok(demandeService.archive(id, archive));
    }

    @GetMapping("/statuts")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Lister les statuts possibles d'une demande")
    public ResponseEntity<List<StatutResponse>> getStatuts() {
        return ResponseEntity.ok(demandeService.getStatuts());
    }
}
