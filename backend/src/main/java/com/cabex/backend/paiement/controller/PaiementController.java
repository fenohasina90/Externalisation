package com.cabex.backend.paiement.controller;

import com.cabex.backend.paiement.dto.CreatePaiementRequest;
import com.cabex.backend.paiement.dto.ModePaiementResponse;
import com.cabex.backend.paiement.dto.PaiementDetailResponse;
import com.cabex.backend.paiement.dto.PaiementResponse;
import com.cabex.backend.paiement.dto.UpdatePaiementRequest;
import com.cabex.backend.paiement.service.PaiementGestionService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@Tag(name = "Paiements", description = "Enregistrement des paiements par l'Économat")
@SecurityRequirement(name = "bearerAuth")
public class PaiementController {

    private final PaiementGestionService paiementService;

    public PaiementController(PaiementGestionService paiementService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAIEMENT_VIEW')")
    @Operation(summary = "Lister / rechercher / filtrer les paiements",
            description = "Pagination, recherche par référence, filtres demande/mode/dates")
    public ResponseEntity<Page<PaiementResponse>> getAll(
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Integer demandeId,
            @RequestParam(required = false) Integer modeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            Pageable pageable) {
        return ResponseEntity.ok(paiementService.getAll(recherche, demandeId, modeId,
                dateDebut, dateFin, pageable));
    }

    @GetMapping("/modes")
    @PreAuthorize("hasAuthority('PAIEMENT_VIEW')")
    @Operation(summary = "Lister les modes de paiement disponibles",
            description = "ESPECES / VIREMENT - la référence de virement est exigée pour VIREMENT")
    public ResponseEntity<List<ModePaiementResponse>> getModes() {
        return ResponseEntity.ok(paiementService.getModes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAIEMENT_VIEW')")
    @Operation(summary = "Consulter le détail d'un paiement",
            description = "Référence, demande, échéance, mode, montant, virement, utilisateur enregistreur")
    public ResponseEntity<PaiementResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(paiementService.getById(id));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAuthority('PAIEMENT_VIEW')")
    @Operation(summary = "Consulter le détail d'un paiement par sa référence (ex. PAY-2026-00020)",
            description = "Reprend les informations du reçu : service et échéance, montants attendu/payé/reste, montant en lettres")
    public ResponseEntity<PaiementDetailResponse> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(paiementService.getByReference(reference));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PAIEMENT_CREATE')")
    @Operation(summary = "Enregistrer un paiement",
            description = "Génère une référence PAY-YYYY-NNNNN. Vérifie montant > 0, demande/échéance/mode valides, montant <= reste, virement avec référence. Met à jour le montant payé de l'échéance.")
    public ResponseEntity<PaiementResponse> create(@Valid @RequestBody CreatePaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paiementService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAIEMENT_UPDATE')")
    @Operation(summary = "Modifier un paiement",
            description = "Recalcule le montant payé / reste et ajuste l'échéance associée")
    public ResponseEntity<PaiementResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody UpdatePaiementRequest request) {
        return ResponseEntity.ok(paiementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAIEMENT_DELETE')")
    @Operation(summary = "Supprimer un paiement",
            description = "Retire le montant payé de l'échéance associée (réservé SUPER_ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        paiementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
