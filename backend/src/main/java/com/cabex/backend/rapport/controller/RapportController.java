package com.cabex.backend.rapport.controller;

import com.cabex.backend.demande.dto.DemandeResponse;
import com.cabex.backend.rapport.dto.RapportClientLigne;
import com.cabex.backend.rapport.dto.RapportFilter;
import com.cabex.backend.rapport.dto.RapportFinancierResponse;
import com.cabex.backend.rapport.dto.RapportGeneralResponse;
import com.cabex.backend.rapport.dto.RapportPaiementLigne;
import com.cabex.backend.rapport.dto.RapportServiceLigne;
import com.cabex.backend.rapport.service.RapportExporter;
import com.cabex.backend.rapport.service.RapportService;
import com.cabex.backend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rapports")
@Tag(name = "Rapports", description = "Rapports et exports (PDF, Excel, CSV)")
@SecurityRequirement(name = "bearerAuth")
public class RapportController {

    private final RapportService rapportService;
    private final RapportExporter rapportExporter;

    public RapportController(RapportService rapportService, RapportExporter rapportExporter) {
        this.rapportService = rapportService;
        this.rapportExporter = rapportExporter;
    }

    @GetMapping("/general")
    @PreAuthorize("hasAuthority('RAPPORT_DEMANDE_VIEW')")
    @Operation(summary = "Rapport général — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<?> general(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer domaineId,
            @RequestParam(required = false) Integer commercialId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String format) {
        RapportFilter f = RapportFilter.of(dateDebut, dateFin, clientId, serviceId, domaineId, commercialId, statut);
        if (format != null && !format.isBlank()) {
            return export("general", f, format);
        }
        return ResponseEntity.ok(rapportService.general(f));
    }

    @GetMapping("/financier")
    @PreAuthorize("hasAuthority('RAPPORT_PAIEMENT_VIEW')")
    @Operation(summary = "Rapport financier — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<?> financier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer domaineId,
            @RequestParam(required = false) Integer commercialId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String format) {
        RapportFilter f = RapportFilter.of(dateDebut, dateFin, clientId, serviceId, domaineId, commercialId, statut);
        if (format != null && !format.isBlank()) {
            return export("financier", f, format);
        }
        return ResponseEntity.ok(rapportService.financier(f));
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('RAPPORT_DEMANDE_VIEW')")
    @Operation(summary = "Rapport par client — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<?> clients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer domaineId,
            @RequestParam(required = false) Integer commercialId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String format) {
        RapportFilter f = RapportFilter.of(dateDebut, dateFin, clientId, serviceId, domaineId, commercialId, statut);
        if (format != null && !format.isBlank()) {
            return export("clients", f, format);
        }
        return ResponseEntity.ok(rapportService.clients(f));
    }

    @GetMapping("/services")
    @PreAuthorize("hasAuthority('RAPPORT_PAIEMENT_VIEW')")
    @Operation(summary = "Rapport par service — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<?> services(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer domaineId,
            @RequestParam(required = false) Integer commercialId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String format) {
        RapportFilter f = RapportFilter.of(dateDebut, dateFin, clientId, serviceId, domaineId, commercialId, statut);
        if (format != null && !format.isBlank()) {
            return export("services", f, format);
        }
        return ResponseEntity.ok(rapportService.services(f));
    }

    @GetMapping("/demandes")
    @PreAuthorize("hasAuthority('RAPPORT_DEMANDE_VIEW')")
    @Operation(summary = "Rapport des demandes — SUPER_ADMIN + ACCUEIL")
    public ResponseEntity<?> demandes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer domaineId,
            @RequestParam(required = false) Integer commercialId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String format) {
        RapportFilter f = RapportFilter.of(dateDebut, dateFin, clientId, serviceId, domaineId, commercialId, statut);
        if (format != null && !format.isBlank()) {
            return export("demandes", f, format);
        }
        return ResponseEntity.ok(rapportService.demandes(f));
    }

    @GetMapping("/paiements")
    @PreAuthorize("hasAuthority('RAPPORT_PAIEMENT_VIEW')")
    @Operation(summary = "Rapport des paiements — SUPER_ADMIN + ECONOMAT")
    public ResponseEntity<?> paiements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer domaineId,
            @RequestParam(required = false) Integer commercialId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String format) {
        RapportFilter f = RapportFilter.of(dateDebut, dateFin, clientId, serviceId, domaineId, commercialId, statut);
        if (format != null && !format.isBlank()) {
            return export("paiements", f, format);
        }
        return ResponseEntity.ok(rapportService.paiements(f));
    }

    private ResponseEntity<byte[]> export(String type, RapportFilter f, String format) {
        String permission = switch (type) {
            case "general", "clients", "demandes" -> "RAPPORT_DEMANDE_EXPORT";
            default -> "RAPPORT_PAIEMENT_EXPORT";
        };
        SecurityUtils.requirePermission(permission);
        byte[] bytes = rapportExporter.exporter(rapportService.table(type, f), format);
        String filename = "rapport-" + type + "." + rapportExporter.extension(format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rapportExporter.contentType(format)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }
}
