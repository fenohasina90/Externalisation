package com.cabex.backend.dashboard.service;

import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.configuration.service.ConfigurationKeys;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.dashboard.dto.ClientRevenuResponse;
import com.cabex.backend.dashboard.dto.CommercialTopResponse;
import com.cabex.backend.dashboard.dto.DashboardFilter;
import com.cabex.backend.dashboard.dto.DashboardFiltreOption;
import com.cabex.backend.dashboard.dto.DashboardFiltresResponse;
import com.cabex.backend.dashboard.dto.DashboardSummaryResponse;
import com.cabex.backend.dashboard.dto.DemandeMoisResponse;
import com.cabex.backend.dashboard.dto.DemandeTermineeMoisResponse;
import com.cabex.backend.dashboard.dto.EcheanceAlerteResponse;
import com.cabex.backend.dashboard.dto.EcheanceDashboardResponse;
import com.cabex.backend.dashboard.dto.ModeMontantResponse;
import com.cabex.backend.dashboard.dto.MoisMontantResponse;
import com.cabex.backend.dashboard.dto.PaiementDashboardResponse;
import com.cabex.backend.dashboard.dto.PerformanceIndicateursResponse;
import com.cabex.backend.dashboard.dto.ServiceRevenuResponse;
import com.cabex.backend.dashboard.dto.ServiceTopResponse;
import com.cabex.backend.dashboard.dto.StatutCountResponse;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardService {

    private final SocieteRepository societeRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final ServiceRepository serviceRepository;
    private final PaiementRepository paiementRepository;
    private final EcheancePaiementRepository echeanceRepository;
    private final ConfigurationService configurationService;

    public DashboardService(SocieteRepository societeRepository,
                            DemandeServiceRepository demandeRepository,
                            HistoriqueDemandeRepository historiqueRepository,
                            ServiceRepository serviceRepository,
                            PaiementRepository paiementRepository,
                            EcheancePaiementRepository echeanceRepository,
                            ConfigurationService configurationService) {
        this.societeRepository = societeRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueRepository = historiqueRepository;
        this.serviceRepository = serviceRepository;
        this.paiementRepository = paiementRepository;
        this.echeanceRepository = echeanceRepository;
        this.configurationService = configurationService;
    }

    private int joursAlerte() {
        return Math.max(configurationService.lireEntier(
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE,
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE_DEFAUT), 1);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(DashboardFilter filtre) {
        LocalDate today = LocalDate.now();
        String role = SecurityUtils.currentRoleName();

        Map<String, Long> parStatut = groupStatuts(filtre);
        long demandesEnCours = parStatut.getOrDefault("EN_COURS", 0L);
        long demandesEnAttente = parStatut.getOrDefault("EN_ATTENTE", 0L);

        if ("ECONOMAT".equalsIgnoreCase(role)) {
            BigDecimal montantTotal = sommeTarif(filtre);
            BigDecimal montantPaye = sommePayee(filtre);
            BigDecimal reste = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);
            return new DashboardSummaryResponse(
                    null, null, null,
                    montantTotal, montantPaye, reste,
                    echeanceRepository.countEnRetardFiltre(today,
                            filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId()),
                    countProches(today, filtre),
                    null, null);
        }

        long clients = filtre.aDesFiltres()
                ? demandeRepository.compterClientsFiltre(filtre.dateDebut(), filtre.dateFin(),
                        filtre.clientId(), filtre.serviceId())
                : societeRepository.countByActifTrue();
        long demandes = demandeRepository.countFiltre(filtre.dateDebut(), filtre.dateFin(),
                filtre.clientId(), filtre.serviceId());
        long services = filtre.aDesFiltres()
                ? demandeRepository.compterServicesFiltre(filtre.dateDebut(), filtre.dateFin(),
                        filtre.clientId(), filtre.serviceId())
                : serviceRepository.countByActifTrue();

        if ("ACCUEIL".equalsIgnoreCase(role)) {
            return new DashboardSummaryResponse(
                    clients, demandes, services,
                    null, null, null,
                    null, null,
                    demandesEnAttente, demandesEnCours);
        }

        BigDecimal montantTotal = sommeTarif(filtre);
        BigDecimal montantPaye = sommePayee(filtre);
        BigDecimal reste = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);

        return new DashboardSummaryResponse(
                clients, demandes, services,
                montantTotal, montantPaye, reste,
                echeanceRepository.countEnRetardFiltre(today,
                        filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId()),
                countProches(today, filtre),
                demandesEnAttente, demandesEnCours);
    }

    @Transactional(readOnly = true)
    public PerformanceIndicateursResponse performances(DashboardFilter filtre) {
        String role = SecurityUtils.currentRoleName();
        BigDecimal tauxPaiementATemps = tauxPaiementATemps(filtre);

        if ("ECONOMAT".equalsIgnoreCase(role)) {
            return new PerformanceIndicateursResponse(
                    null, null, null, null, tauxPaiementATemps,
                    null, null, List.of());
        }

        List<Object[]> termeinees = historiqueRepository.termeineesEtDatesFiltrees(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        long nbTerminees = termeinees.size();
        long totalDemandes = demandeRepository.countFiltre(filtre.dateDebut(), filtre.dateFin(),
                filtre.clientId(), filtre.serviceId());

        BigDecimal delaiMoyen = BigDecimal.ZERO;
        if (nbTerminees > 0) {
            long totalJours = 0;
            for (Object[] o : termeinees) {
                LocalDate dateDemande = (LocalDate) o[1];
                LocalDateTime termineeAt = (LocalDateTime) o[2];
                totalJours += ChronoUnit.DAYS.between(dateDemande, termineeAt.toLocalDate());
            }
            delaiMoyen = BigDecimal.valueOf(totalJours)
                    .divide(BigDecimal.valueOf(nbTerminees), 2, RoundingMode.HALF_UP);
        }

        BigDecimal tauxTerminees = totalDemandes == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(nbTerminees * 100.0 / totalDemandes)
                        .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montantTotal = sommeTarif(filtre);
        BigDecimal montantMoyen = totalDemandes == 0 ? BigDecimal.ZERO
                : montantTotal.divide(BigDecimal.valueOf(totalDemandes), 2, RoundingMode.HALF_UP);

        ServiceTopResponse serviceTop = null;
        List<Object[]> services = demandeRepository.compterParServiceFiltre(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        if (!services.isEmpty()) {
            Object[] s = services.get(0);
            serviceTop = new ServiceTopResponse((Integer) s[0], (String) s[1], toLong(s[2]));
        }

        CommercialTopResponse commercialTop = null;
        List<Object[]> commerciaux = demandeRepository.compterParCommercialFiltre(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        if (!commerciaux.isEmpty()) {
            Object[] c = commerciaux.get(0);
            commercialTop = new CommercialTopResponse((Integer) c[0], (String) c[1], toLong(c[2]));
        }

        Map<YearMonth, Long> parMois = new TreeMap<>();
        for (Object[] o : termeinees) {
            LocalDateTime t = (LocalDateTime) o[2];
            parMois.merge(YearMonth.of(t.getYear(), t.getMonthValue()), 1L, Long::sum);
        }
        List<DemandeTermineeMoisResponse> parMoisList = parMois.entrySet().stream()
                .map(e -> new DemandeTermineeMoisResponse(e.getKey().toString(), e.getValue()))
                .toList();

        if ("ACCUEIL".equalsIgnoreCase(role)) {
            return new PerformanceIndicateursResponse(
                    delaiMoyen, nbTerminees, tauxTerminees, montantMoyen, null,
                    serviceTop, commercialTop, parMoisList);
        }

        return new PerformanceIndicateursResponse(
                delaiMoyen, nbTerminees, tauxTerminees, montantMoyen, tauxPaiementATemps,
                serviceTop, commercialTop, parMoisList);
    }

    @Transactional(readOnly = true)
    public List<StatutCountResponse> demandesParStatut(DashboardFilter filtre) {
        return groupStatuts(filtre).entrySet().stream()
                .map(e -> new StatutCountResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatutCountResponse::statut))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaiementDashboardResponse paiements(DashboardFilter filtre) {
        List<ModeMontantResponse> parMode = paiementRepository.sommeParMode(
                        filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId(), null)
                .stream()
                .map(o -> new ModeMontantResponse(
                        (String) o[0], toBigDecimal(o[1])))
                .sorted(Comparator.comparing(ModeMontantResponse::mode))
                .toList();

        Map<YearMonth, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] o : paiementRepository.montantsParMoisFiltre(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())) {
            YearMonth ym = YearMonth.of((int) o[0], (int) o[1]);
            map.merge(ym, toBigDecimal(o[2]), BigDecimal::add);
        }
        List<MoisMontantResponse> parMois = map.entrySet().stream()
                .map(e -> new MoisMontantResponse(e.getKey().toString(), e.getValue()))
                .toList();

        return new PaiementDashboardResponse(parMode, parMois);
    }

    @Transactional(readOnly = true)
    public EcheanceDashboardResponse echeances(DashboardFilter filtre) {
        LocalDate today = LocalDate.now();
        LocalDate limite = today.plusDays(joursAlerte());
        List<EcheanceAlerteResponse> enRetard = echeanceRepository.findEnRetardFiltre(
                        today, filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
                .stream()
                .map(e -> toAlerte(e, "EN_RETARD"))
                .toList();
        List<EcheanceAlerteResponse> aVenir = echeanceRepository.findAVenirFiltre(
                        today, filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
                .stream()
                .filter(e -> !e.getDateEcheance().isAfter(limite))
                .map(e -> toAlerte(e, "PROCHE"))
                .toList();
        return new EcheanceDashboardResponse(enRetard, aVenir);
    }

    @Transactional(readOnly = true)
    public List<ServiceRevenuResponse> services(DashboardFilter filtre) {
        return demandeRepository.revenuParServiceFiltre(
                        filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
                .stream()
                .map(o -> new ServiceRevenuResponse(
                        (Integer) o[0],
                        (String) o[1],
                        toLong(o[2]),
                        toBigDecimal(o[3])))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientRevenuResponse> clients(DashboardFilter filtre) {
        String role = SecurityUtils.currentRoleName();
        List<Object[]> demandes = demandeRepository.demandesParSocieteFiltre(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());

        if ("ACCUEIL".equalsIgnoreCase(role)) {
            return demandes.stream()
                    .map(o -> new ClientRevenuResponse(
                            (Integer) o[0], (String) o[1], toLong(o[2]), null, null))
                    .toList();
        }

        Map<Integer, BigDecimal> payeParClient = new HashMap<>();
        for (Object[] o : paiementRepository.sommeParSocieteFiltre(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())) {
            if (o[0] != null) {
                payeParClient.put((Integer) o[0], toBigDecimal(o[1]));
            }
        }
        return demandes.stream()
                .map(o -> {
                    Integer clientId = (Integer) o[0];
                    String nom = (String) o[1];
                    long nb = toLong(o[2]);
                    BigDecimal total = toBigDecimal(o[3]);
                    BigDecimal paye = payeParClient.getOrDefault(clientId, BigDecimal.ZERO);
                    if ("ECONOMAT".equalsIgnoreCase(role)) {
                        return new ClientRevenuResponse(clientId, nom, null, total, paye);
                    }
                    return new ClientRevenuResponse(clientId, nom, nb, total, paye);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeMoisResponse> demandesParMois(DashboardFilter filtre) {
        return demandeRepository.compterParMoisFiltre(
                        filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
                .stream()
                .map(o -> new DemandeMoisResponse(
                        YearMonth.of((int) o[0], (int) o[1]).toString(), toLong(o[2])))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceTopResponse> demandesParService(DashboardFilter filtre) {
        return demandeRepository.compterParServiceFiltre(
                        filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
                .stream()
                .map(o -> new ServiceTopResponse((Integer) o[0], (String) o[1], toLong(o[2])))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardFiltresResponse filtres() {
        List<DashboardFiltreOption> clients = societeRepository.findActivesPourFiltre().stream()
                .map(s -> new DashboardFiltreOption(s.getId(), s.getNomSociete()))
                .toList();
        List<DashboardFiltreOption> services = serviceRepository.findActifsPourFiltre().stream()
                .map(s -> new DashboardFiltreOption(s.getId(), s.getNomService()))
                .toList();
        return new DashboardFiltresResponse(clients, services);
    }

    private Map<String, Long> groupStatuts(DashboardFilter filtre) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] o : demandeRepository.compterParStatutFiltre(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())) {
            map.put((String) o[0], toLong(o[1]));
        }
        return map;
    }

    private long countProches(LocalDate today, DashboardFilter filtre) {
        return echeanceRepository.countProchesFiltre(today, today.plusDays(joursAlerte()),
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
    }

    private BigDecimal sommeTarif(DashboardFilter filtre) {
        BigDecimal montant = demandeRepository.sommeTarifDashboard(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        return montant == null ? BigDecimal.ZERO : montant;
    }

    private BigDecimal sommePayee(DashboardFilter filtre) {
        BigDecimal montant = paiementRepository.sommePayee(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId(), null);
        return montant == null ? BigDecimal.ZERO : montant;
    }

    private BigDecimal tauxPaiementATemps(DashboardFilter filtre) {
        List<Object[]> stats = paiementRepository.statistiquesPaiementsEcheance(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        Object[] ligne = stats.isEmpty() ? null : stats.get(0);
        long totalPayts = ligne == null ? 0 : toLong(ligne[0]);
        long aTemps = ligne == null || ligne.length < 2 ? 0 : toLong(ligne[1]);
        return totalPayts == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(aTemps * 100.0 / totalPayts).setScale(2, RoundingMode.HALF_UP);
    }

    private EcheanceAlerteResponse toAlerte(EcheancePaiement e, String statut) {
        BigDecimal montantAttendu = e.getMontantAttendu();
        BigDecimal montantPaye = e.getMontantPaye() == null ? BigDecimal.ZERO : e.getMontantPaye();
        return new EcheanceAlerteResponse(
                e.getId().longValue(),
                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                        && e.getProposPaiement().getDemande().getSociete() != null
                        ? e.getProposPaiement().getDemande().getSociete().getId() : null,
                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                        && e.getProposPaiement().getDemande().getSociete() != null
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

    private long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }
}
