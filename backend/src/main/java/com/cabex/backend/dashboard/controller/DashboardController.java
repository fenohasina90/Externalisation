package com.cabex.backend.dashboard.controller;

import com.cabex.backend.dashboard.dto.ClientRevenuResponse;
import com.cabex.backend.dashboard.dto.DashboardSummaryResponse;
import com.cabex.backend.dashboard.dto.DemandeMoisResponse;
import com.cabex.backend.dashboard.dto.EcheanceDashboardResponse;
import com.cabex.backend.dashboard.dto.PaiementDashboardResponse;
import com.cabex.backend.dashboard.dto.PerformanceIndicateursResponse;
import com.cabex.backend.dashboard.dto.ServiceRevenuResponse;
import com.cabex.backend.dashboard.dto.StatutCountResponse;
import com.cabex.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<DashboardSummaryResponse> summary() {
        return ResponseEntity.ok(dashboardService.summary());
    }

    @GetMapping("/performances")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Indicateurs de performance (personnalisés selon le rôle)")
    public ResponseEntity<PerformanceIndicateursResponse> performances() {
        return ResponseEntity.ok(dashboardService.performances());
    }

    @GetMapping("/demandes")
    @PreAuthorize("hasAuthority('DASHBOARD_DEMANDE_VIEW')")
    @Operation(summary = "Statistiques des demandes (par statut) — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<List<StatutCountResponse>> demandes() {
        return ResponseEntity.ok(dashboardService.demandesParStatut());
    }

    @GetMapping("/demandes/mois")
    @PreAuthorize("hasAuthority('DASHBOARD_DEMANDE_VIEW')")
    @Operation(summary = "Nombre de demandes par mois — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<List<DemandeMoisResponse>> demandesParMois() {
        return ResponseEntity.ok(dashboardService.demandesParMois());
    }

    @GetMapping("/paiements")
    @PreAuthorize("hasAuthority('DASHBOARD_PAIEMENT_VIEW')")
    @Operation(summary = "Statistiques des paiements (par mode et par mois) — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<PaiementDashboardResponse> paiements() {
        return ResponseEntity.ok(dashboardService.paiements());
    }

    @GetMapping("/echeances")
    @PreAuthorize("hasAuthority('DASHBOARD_PAIEMENT_VIEW')")
    @Operation(summary = "Échéances en retard et à venir — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<EcheanceDashboardResponse> echeances() {
        return ResponseEntity.ok(dashboardService.echeances());
    }

    @GetMapping("/services")
    @PreAuthorize("hasAuthority('DASHBOARD_PAIEMENT_VIEW')")
    @Operation(summary = "Revenus par service — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<List<ServiceRevenuResponse>> services() {
        return ResponseEntity.ok(dashboardService.services());
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Top clients (personnalisé selon le rôle)")
    public ResponseEntity<List<ClientRevenuResponse>> clients() {
        return ResponseEntity.ok(dashboardService.clients());
    }
}
