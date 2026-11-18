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
import com.cabex.backend.dashboard.dto.PaiementRetardResponse;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.dashboard.dto.ModeMontantResponse;
import com.cabex.backend.dashboard.dto.MoisMontantResponse;
import com.cabex.backend.dashboard.dto.PaiementDashboardResponse;
import com.cabex.backend.dashboard.dto.PerformanceIndicateursResponse;
import com.cabex.backend.dashboard.dto.ServiceRevenuResponse;
import com.cabex.backend.dashboard.dto.ServiceTopResponse;
import com.cabex.backend.dashboard.dto.StatutCountResponse;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardService {

    /** Fausse ligne « Débours » (aucun service en base) — toujours affichée. */
    static final int ID_DEBOURS = 0;
    static final String NOM_DEBOURS = "Débours";


    private final SocieteRepository societeRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final ServiceRepository serviceRepository;
    private final PaiementRepository paiementRepository;
    private final LigneServiceRepository ligneServiceRepository;
    private final FactureValideeRepository factureValideeRepository;
    private final ConfigurationService configurationService;

    public DashboardService(SocieteRepository societeRepository,
                            DemandeServiceRepository demandeRepository,
                            HistoriqueDemandeRepository historiqueRepository,
                            ServiceRepository serviceRepository,
                            PaiementRepository paiementRepository,
                            LigneServiceRepository ligneServiceRepository,
                            FactureValideeRepository factureValideeRepository,
                            ConfigurationService configurationService) {
        this.societeRepository = societeRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueRepository = historiqueRepository;
        this.serviceRepository = serviceRepository;
        this.paiementRepository = paiementRepository;
        this.ligneServiceRepository = ligneServiceRepository;
        this.factureValideeRepository = factureValideeRepository;
        this.configurationService = configurationService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(DashboardFilter filtre) {
        LocalDate today = LocalDate.now();
        String role = SecurityUtils.currentRoleName();

        Map<String, Long> parStatut = groupStatuts(filtre);
        long demandesEnCours = parStatut.getOrDefault("EN_COURS", 0L);
        long demandesEnAttente = parStatut.getOrDefault("EN_ATTENTE", 0L);

        if ("ECONOMAT".equalsIgnoreCase(role)) {
            BigDecimal montantTotal = sommeFacturesPayables(filtre);
            BigDecimal montantPaye = sommePayee(filtre);
            BigDecimal reste = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);
            return new DashboardSummaryResponse(
                    null, null, null,
                    montantTotal, montantPaye, reste,
                    Long.valueOf(paiementsEnRetard(today, filtre).size()),
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
                    null,
                    demandesEnAttente, demandesEnCours);
        }

        BigDecimal montantTotal = sommeFacturesPayables(filtre);
        BigDecimal montantPaye = sommePayee(filtre);
        BigDecimal reste = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);

        return new DashboardSummaryResponse(
                clients, demandes, services,
                montantTotal, montantPaye, reste,
                Long.valueOf(paiementsEnRetard(today, filtre).size()),
                demandesEnAttente, demandesEnCours);
    }

    @Transactional(readOnly = true)
    public PerformanceIndicateursResponse performances(DashboardFilter filtre) {
        String role = SecurityUtils.currentRoleName();

        if ("ECONOMAT".equalsIgnoreCase(role)) {
            return new PerformanceIndicateursResponse(
                    null, null,
                    null, null,
                    List.of());
        }

        List<Object[]> termeinees = historiqueRepository.termeineesEtDatesFiltrees(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        long nbTerminees = termeinees.size();
        long totalDemandes = demandeRepository.countFiltre(filtre.dateDebut(), filtre.dateFin(),
                filtre.clientId(), filtre.serviceId());

        BigDecimal tauxTerminees = totalDemandes == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(nbTerminees * 100.0 / totalDemandes)
                        .setScale(2, RoundingMode.HALF_UP);

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
                    nbTerminees, tauxTerminees,
                    serviceTop, commercialTop, parMoisList);
        }

        return new PerformanceIndicateursResponse(
                nbTerminees, tauxTerminees, serviceTop, commercialTop, parMoisList);
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
                        filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
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

    /**
     * Paiements en retard — factures validées non soldées, au-delà des délais
     * d'alerte configurés (prestation/débours ou mensuelle). La date limite
     * est la date d'émission de la facture validée.
     */
    @Transactional(readOnly = true)
    public List<PaiementRetardResponse> paiementsEnRetard(DashboardFilter filtre) {
        return paiementsEnRetard(LocalDate.now(), filtre);
    }

    private List<PaiementRetardResponse> paiementsEnRetard(LocalDate aujourdhui,
                                                           DashboardFilter filtre) {
        int pdJours = Math.max(0, configurationService.lireEntier(
                ConfigurationKeys.JOURS_RETARD_PRESTATION_DEBOURS,
                ConfigurationKeys.JOURS_RETARD_PRESTATION_DEBOURS_DEFAUT));
        int aboJours = Math.max(0, configurationService.lireEntier(
                ConfigurationKeys.JOURS_RETARD_ABONNEMENT,
                ConfigurationKeys.JOURS_RETARD_ABONNEMENT_DEFAUT));
        LocalDate limitePrestationDebours = aujourdhui.minusDays(pdJours);
        LocalDate limiteAbonnement = aujourdhui.minusDays(aboJours);

        List<com.cabex.backend.facture.entity.FactureValidee> candidates = new java.util.ArrayList<>();
        candidates.addAll(factureValideeRepository.findFacturesPourAlertesPaiement());
        candidates.addAll(factureValideeRepository.findFacturesMensuellesPourAlerte());
        List<PaiementRetardResponse> resultat = new java.util.ArrayList<>();
        for (com.cabex.backend.facture.entity.FactureValidee f : candidates) {
            if (f.getValideLe() == null) {
                continue;
            }
            LocalDate emission = f.getValideLe().toLocalDate();
            boolean mensuelle = f.getMois() != null;
            if (mensuelle) {
                if (emission.isAfter(limiteAbonnement)) continue;
            } else {
                if (emission.isAfter(limitePrestationDebours)) continue;
            }
            if (filtre.dateDebut() != null && emission.isBefore(filtre.dateDebut())) continue;
            if (filtre.dateFin() != null && emission.isAfter(filtre.dateFin())) continue;
            com.cabex.backend.demande.entity.DemandeService d = f.getDemande();
            Integer societeId = d != null && d.getSociete() != null ? d.getSociete().getId() : null;
            if (filtre.clientId() != null && !filtre.clientId().equals(societeId)) continue;
            Integer demandeId = d != null ? d.getId() : null;
            if (filtre.serviceId() != null) {
                if (demandeId == null) continue;
                if (!ligneServiceRepository.existsByDemandeIdAndServiceId(demandeId, filtre.serviceId())) {
                    continue;
                }
            }
            BigDecimal payee = paiementRepository.sommePayeeParFactureValidee(f.getId());
            if (payee == null) payee = BigDecimal.ZERO;
            BigDecimal montant = f.getMontant() != null ? f.getMontant() : BigDecimal.ZERO;
            BigDecimal reste = montant.subtract(payee);
            if (reste.signum() <= 0) continue;
            String nomSociete = d != null && d.getSociete() != null ? d.getSociete().getNomSociete() : null;
            String referenceDemande = d != null ? d.getReferenceDemande() : null;
            resultat.add(new PaiementRetardResponse(
                    f.getId() != null ? f.getId().longValue() : null,
                    societeId,
                    nomSociete,
                    demandeId,
                    referenceDemande,
                    emission,
                    montant,
                    payee,
                    reste,
                    "EN_RETARD"));
        }
        resultat.sort(java.util.Comparator.comparing(PaiementRetardResponse::reste).reversed());
        return resultat;
    }

    @Transactional(readOnly = true)
    public List<ServiceRevenuResponse> services(DashboardFilter filtre) {
        List<ServiceRevenuResponse> resultat = new java.util.ArrayList<>(
                factureValideeRepository.sommeServiceFacturesValidees(
                                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
                        .stream()
                        .map(o -> new ServiceRevenuResponse(
                                (Integer) o[0],
                                (String) o[1],
                                toLong(o[2]),
                                toBigDecimal(o[3])))
                        .toList());
        // Débours : ligne toujours présente ; ajoutée si la requête ne l'a pas renvoyée (garde-fou).
        boolean deboursPresent = resultat.stream().anyMatch(r -> r.serviceId() == ID_DEBOURS);
        if (!deboursPresent) {
            long nb = demandeRepository.compterDemandesAvecDeboursFiltre(
                    filtre.dateDebut(), filtre.dateFin(), filtre.clientId());
            resultat.add(new ServiceRevenuResponse(ID_DEBOURS, NOM_DEBOURS, nb, BigDecimal.ZERO));
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public List<ClientRevenuResponse> clients(DashboardFilter filtre) {
        String role = SecurityUtils.currentRoleName();
        List<Object[]> demandes = factureValideeRepository.sommeMontantFacturesParClient(
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
        List<ServiceTopResponse> resultat = new java.util.ArrayList<>(
                demandeRepository.compterParServiceFiltre(
                                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId())
                        .stream()
                        .map(o -> new ServiceTopResponse((Integer) o[0], (String) o[1], toLong(o[2])))
                        .toList());
        // Débours : toujours figurées, quel que soit le filtre service.
        long nbDebours = demandeRepository.compterDemandesAvecDeboursFiltre(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId());
        resultat.add(new ServiceTopResponse(ID_DEBOURS, NOM_DEBOURS, nbDebours));
        return resultat;
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

    /**
     * Chiffre d'affaires prévu — somme des factures validées <b>payables</b>
     * (enfant du mode UNIQUE, facture PAR_SERVICE, mensualité d'abonnement),
     * <b>hors débours</b> (seules prestation et abonnement), et hors demandes
     * encore en attente de validation. Aucune facture générale UNIQUE comptabilisée.
     * Identique à la règle « cible de paiement ».
     */
    private BigDecimal sommeFacturesPayables(DashboardFilter filtre) {
        BigDecimal montant = factureValideeRepository.sommeMontantFacturesPayables(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        return montant == null ? BigDecimal.ZERO : montant;
    }

    /**
     * Montant payé lié aux factures validées <b>payables</b> (prestation + abonnement) —
     * hors débours et hors facture générale UNIQUE, comme le total prévu.
     */
    private BigDecimal sommePayee(DashboardFilter filtre) {
        BigDecimal montant = paiementRepository.sommePayeeFacturesPayables(
                filtre.dateDebut(), filtre.dateFin(), filtre.clientId(), filtre.serviceId());
        return montant == null ? BigDecimal.ZERO : montant;
    }

    private BigDecimal toBigDecimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }

    private long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }
}
