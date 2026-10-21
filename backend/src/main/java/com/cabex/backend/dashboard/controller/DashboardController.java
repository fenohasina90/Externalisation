package com.cabex.backend.dashboard.controller;

import com.cabex.backend.dashboard.dto.ClientRevenuResponse;
import com.cabex.backend.dashboard.dto.DashboardFilter;
import com.cabex.backend.dashboard.dto.DashboardFiltresResponse;
import com.cabex.backend.dashboard.dto.DashboardSummaryResponse;
import com.cabex.backend.dashboard.dto.DemandeMoisResponse;
import com.cabex.backend.dashboard.dto.EcheanceDashboardResponse;
import com.cabex.backend.dashboard.dto.PaiementDashboardResponse;
import com.cabex.backend.dashboard.dto.PerformanceIndicateursResponse;
import com.cabex.backend.dashboard.dto.ServiceRevenuResponse;
import com.cabex.backend.dashboard.dto.ServiceTopResponse;
import com.cabex.backend.dashboard.dto.StatutCountResponse;
import com.cabex.backend.dashboard.service.DashboardService;
import com.cabex.backend.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "KPIs, statistiques et graphiques")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Résumé des KPI principaux (personnalisé selon le rôle)")
    public ResponseEntity<DashboardSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.summary(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/performances")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Indicateurs de performance (personnalisés selon le rôle)")
    public ResponseEntity<PerformanceIndicateursResponse> performances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.performances(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/demandes")
    @PreAuthorize("hasAuthority('DASHBOARD_DEMANDE_VIEW')")
    @Operation(summary = "Statistiques des demandes (par statut) — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<List<StatutCountResponse>> demandes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.demandesParStatut(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/demandes/mois")
    @PreAuthorize("hasAuthority('DASHBOARD_DEMANDE_VIEW')")
    @Operation(summary = "Nombre de demandes par mois — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<List<DemandeMoisResponse>> demandesParMois(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.demandesParMois(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/demandes/services")
    @PreAuthorize("hasAuthority('DASHBOARD_DEMANDE_VIEW')")
    @Operation(summary = "Demandes par service (sans données financières) — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<List<ServiceTopResponse>> demandesParService(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.demandesParService(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/paiements")
    @PreAuthorize("hasAuthority('DASHBOARD_PAIEMENT_VIEW')")
    @Operation(summary = "Statistiques des paiements (par mode et par mois) — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<PaiementDashboardResponse> paiements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.paiements(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/echeances")
    @PreAuthorize("hasAuthority('DASHBOARD_PAIEMENT_VIEW')")
    @Operation(summary = "Échéances en retard et à venir — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<EcheanceDashboardResponse> echeances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.echeances(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/services")
    @PreAuthorize("hasAuthority('DASHBOARD_PAIEMENT_VIEW')")
    @Operation(summary = "Revenus par service — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<List<ServiceRevenuResponse>> services(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.services(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Top clients (personnalisé selon le rôle)")
    public ResponseEntity<List<ClientRevenuResponse>> clients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId) {
        return ResponseEntity.ok(dashboardService.clients(filtre(dateDebut, dateFin, clientId, serviceId)));
    }

    @GetMapping("/filtres")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Options des filtres : clients actifs et services actifs")
    public ResponseEntity<DashboardFiltresResponse> filtres() {
        return ResponseEntity.ok(dashboardService.filtres());
    }

    private DashboardFilter filtre(LocalDate dateDebut, LocalDate dateFin,
                                   Integer clientId, Integer serviceId) {
        if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
            throw new BadRequestException("La date de début doit être antérieure ou égale à la date de fin");
        }
        return new DashboardFilter(dateDebut, dateFin, clientId, serviceId);
    }
}
