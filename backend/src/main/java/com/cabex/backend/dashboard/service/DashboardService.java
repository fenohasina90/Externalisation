package com.cabex.backend.dashboard.service;

import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.dashboard.dto.ClientRevenuResponse;
import com.cabex.backend.dashboard.dto.DashboardSummaryResponse;
import com.cabex.backend.dashboard.dto.DemandeMoisResponse;
import com.cabex.backend.dashboard.dto.EcheanceAlerteResponse;
import com.cabex.backend.dashboard.dto.EcheanceDashboardResponse;
import com.cabex.backend.dashboard.dto.ModeMontantResponse;
import com.cabex.backend.dashboard.dto.MoisMontantResponse;
import com.cabex.backend.dashboard.dto.PaiementDashboardResponse;
import com.cabex.backend.dashboard.dto.ServiceRevenuResponse;
import com.cabex.backend.dashboard.dto.StatutCountResponse;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.service.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int JOURS_ALERTE = 3;

    private final SocieteRepository societeRepository;
    private final DemandeServiceRepository demandeRepository;
    private final ServiceRepository serviceRepository;
    private final PaiementRepository paiementRepository;
    private final EcheancePaiementRepository echeanceRepository;

    public DashboardService(SocieteRepository societeRepository,
                            DemandeServiceRepository demandeRepository,
                            ServiceRepository serviceRepository,
                            PaiementRepository paiementRepository,
                            EcheancePaiementRepository echeanceRepository) {
        this.societeRepository = societeRepository;
        this.demandeRepository = demandeRepository;
        this.serviceRepository = serviceRepository;
        this.paiementRepository = paiementRepository;
        this.echeanceRepository = echeanceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        BigDecimal montantTotal = demandeRepository.sommeTarifTotal(null, null, null, null, null, null);
        BigDecimal montantPaye = paiementRepository.sommeTotalePayee();
        if (montantTotal == null) montantTotal = BigDecimal.ZERO;
        if (montantPaye == null) montantPaye = BigDecimal.ZERO;
        BigDecimal reste = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);

        Map<String, Long> parStatut = groupStatuts();
        long demandesEnCours = parStatut.getOrDefault("EN_COURS", 0L);
        long demandesEnAttente = parStatut.getOrDefault("EN_ATTENTE", 0L);

        return new DashboardSummaryResponse(
                societeRepository.countByActifTrue(),
                demandeRepository.countByActifTrue(),
                serviceRepository.countByActifTrue(),
                montantTotal,
                montantPaye,
                reste,
                echeanceRepository.countEnRetard(today),
                countProches(today),
                demandesEnAttente,
                demandesEnCours
        );
    }

    @Transactional(readOnly = true)
    public List<StatutCountResponse> demandesParStatut() {
        return groupStatuts().entrySet().stream()
                .map(e -> new StatutCountResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatutCountResponse::statut))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaiementDashboardResponse paiements() {
        List<ModeMontantResponse> parMode = paiementRepository.sommeParMode(null, null, null, null, null)
                .stream()
                .map(o -> new ModeMontantResponse(
                        (String) o[0], toBigDecimal(o[1])))
                .sorted(Comparator.comparing(ModeMontantResponse::mode))
                .toList();

        Map<YearMonth, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] o : paiementRepository.montantsParMois()) {
            YearMonth ym = YearMonth.of((int) o[0], (int) o[1]);
            map.merge(ym, toBigDecimal(o[2]), BigDecimal::add);
        }
        List<MoisMontantResponse> parMois = map.entrySet().stream()
                .map(e -> new MoisMontantResponse(e.getKey().toString(), e.getValue()))
                .toList();

        return new PaiementDashboardResponse(parMode, parMois);
    }

    @Transactional(readOnly = true)
    public EcheanceDashboardResponse echeances() {
        LocalDate today = LocalDate.now();
        List<EcheanceAlerteResponse> enRetard = echeanceRepository.findEnRetard(today).stream()
                .map(e -> toAlerte(e, "EN_RETARD"))
                .toList();
        List<EcheanceAlerteResponse> aVenir = echeanceRepository.findAVenir(today).stream()
                .filter(e -> !e.getDateEcheance().isAfter(today.plusDays(JOURS_ALERTE)))
                .map(e -> toAlerte(e, "PROCHE"))
                .toList();
        return new EcheanceDashboardResponse(enRetard, aVenir);
    }

    @Transactional(readOnly = true)
    public List<ServiceRevenuResponse> services() {
        return demandeRepository.revenuParService().stream()
                .map(o -> new ServiceRevenuResponse(
                        (Integer) o[0],
                        (String) o[1],
                        (long) o[2],
                        toBigDecimal(o[3])))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientRevenuResponse> clients() {
        return demandeRepository.demandesParSociete().stream()
                .map(o -> {
                    Integer clientId = (Integer) o[0];
                    String nom = (String) o[1];
                    long nb = (long) o[2];
                    BigDecimal total = toBigDecimal(o[3]);
                    BigDecimal paye = paiementRepository.sommePayee(null, null, clientId, null, null);
                    return new ClientRevenuResponse(clientId, nom, nb, total, paye);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeMoisResponse> demandesParMois() {
        return demandeRepository.compterParMois().stream()
                .map(o -> new DemandeMoisResponse(
                        YearMonth.of((int) o[0], (int) o[1]).toString(), (long) o[2]))
                .toList();
    }

    private Map<String, Long> groupStatuts() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] o : demandeRepository.compterParStatut()) {
            map.put((String) o[0], (long) o[1]);
        }
        return map;
    }

    private long countProches(LocalDate today) {
        return echeanceRepository.findAVenir(today).stream()
                .filter(e -> !e.getDateEcheance().isAfter(today.plusDays(JOURS_ALERTE)))
                .count();
    }

    private EcheanceAlerteResponse toAlerte(EcheancePaiement e, String statut) {
        BigDecimal montantAttendu = e.getMontantAttendu();
        BigDecimal montantPaye = e.getMontantPaye() == null ? BigDecimal.ZERO : e.getMontantPaye();
        return new EcheanceAlerteResponse(
                e.getId().longValue(),
                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                        ? e.getProposPaiement().getDemande().getSociete().getId() : null,
                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                        ? e.getProposPaiement().getDemande().getSociete().getNomSociete() : null,
                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                        ? e.getProposPaiement().getDemande().getId() : null,
                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                        ? e.getProposPaiement().getDemande().getReferenceDemande() : null,
                e.getDateEcheance(),
                montantAttendu,
                montantPaye,
                montantAttendu.subtract(montantPaye),
                statut
        );
    }

    private BigDecimal toBigDecimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
