package com.cabex.backend.rapport.service;

import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.demande.dto.DemandeResponse;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.mapper.DemandeMapper;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.rapport.dto.RapportClientLigne;
import com.cabex.backend.rapport.dto.RapportFilter;
import com.cabex.backend.rapport.dto.RapportFinancierMode;
import com.cabex.backend.rapport.dto.RapportFinancierResponse;
import com.cabex.backend.rapport.dto.RapportGeneralResponse;
import com.cabex.backend.rapport.dto.RapportPaiementLigne;
import com.cabex.backend.rapport.dto.RapportServiceLigne;
import com.cabex.backend.service.repository.ServiceRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RapportService {

    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final PaiementRepository paiementRepository;
    private final EcheancePaiementRepository echeanceRepository;
    private final SocieteRepository societeRepository;
    private final ServiceRepository serviceRepository;

    public RapportService(DemandeServiceRepository demandeRepository,
                          HistoriqueDemandeRepository historiqueRepository,
                          PaiementRepository paiementRepository,
                          EcheancePaiementRepository echeanceRepository,
                          SocieteRepository societeRepository,
                          ServiceRepository serviceRepository) {
        this.demandeRepository = demandeRepository;
        this.historiqueRepository = historiqueRepository;
        this.paiementRepository = paiementRepository;
        this.echeanceRepository = echeanceRepository;
        this.societeRepository = societeRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public RapportGeneralResponse general(RapportFilter f) {
        List<DemandeService> demandes = demandesFiltrees(f);
        BigDecimal encaisse = fin(f);
        BigDecimal total = sommeTarif(demandes);
        return new RapportGeneralResponse(
                societeRepository.countByActifTrue(),
                demandes.size(),
                serviceRepository.countByActifTrue(),
                total,
                encaisse,
                total.subtract(encaisse).max(BigDecimal.ZERO),
                echeanceRepository.sommeEnRetardFiltree(LocalDate.now(), f.clientId(), f.serviceId(), f.domaineId()),
                echeanceRepository.countEnRetard(LocalDate.now())
        );
    }

    @Transactional(readOnly = true)
    public RapportFinancierResponse financier(RapportFilter f) {
        List<DemandeService> demandes = demandesFiltrees(f);
        BigDecimal total = sommeTarif(demandes);
        BigDecimal encaisse = fin(f);
        BigDecimal reste = total.subtract(encaisse).max(BigDecimal.ZERO);
        BigDecimal enRetard = echeanceRepository.sommeEnRetardFiltree(LocalDate.now(), f.clientId(), f.serviceId(), f.domaineId());
        List<RapportFinancierMode> parMode = paiementRepository
                .sommeParMode(f.dateDebut(), f.dateFin(), f.clientId(), f.serviceId(), f.domaineId())
                .stream()
                .map(o -> new RapportFinancierMode((String) o[0], toBigDecimal(o[1])))
                .sorted(Comparator.comparing(RapportFinancierMode::mode))
                .toList();
        return new RapportFinancierResponse(total, encaisse, reste, enRetard, parMode);
    }

    @Transactional(readOnly = true)
    public List<RapportClientLigne> clients(RapportFilter f) {
        Map<Integer, List<DemandeService>> parClient = new LinkedHashMap<>();
        for (DemandeService d : demandesFiltrees(f)) {
            // Demandes de création d'entreprise : pas encore de société liée.
            Integer cle = d.getSociete() != null ? d.getSociete().getId() : null;
            parClient.computeIfAbsent(cle, k -> new ArrayList<>()).add(d);
        }
        return parClient.entrySet().stream().map(e -> {
            Integer clientId = e.getKey();
            List<DemandeService> list = e.getValue();
            BigDecimal total = sommeTarif(list);
            BigDecimal paye = clientId == null
                    ? list.stream()
                            .map(d -> toBigDecimal(paiementRepository.sommePayeeParDemande(d.getId())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : fin(f, clientId);
            String nom = clientId == null
                    ? "Création d'entreprise (sans société)"
                    : list.get(0).getSociete().getNomSociete();
            return new RapportClientLigne(clientId,
                    nom,
                    list.size(), total, paye, total.subtract(paye).max(BigDecimal.ZERO));
        }).sorted(Comparator.comparingLong(RapportClientLigne::nombreDemandes).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<RapportServiceLigne> services(RapportFilter f) {
        Map<Integer, List<DemandeService>> parService = new LinkedHashMap<>();
        for (DemandeService d : demandesFiltrees(f)) {
            parService.computeIfAbsent(d.getService().getId(), k -> new ArrayList<>()).add(d);
        }
        return parService.entrySet().stream().map(e -> {
            Integer serviceId = e.getKey();
            List<DemandeService> list = e.getValue();
            return new RapportServiceLigne(serviceId,
                    list.get(0).getService().getNomService(),
                    list.get(0).getSociete() != null && list.get(0).getSociete().getDomaine() != null
                            ? list.get(0).getSociete().getDomaine().getNomDomaine() : null,
                    list.size(), sommeTarif(list));
        }).sorted(Comparator.comparing(RapportServiceLigne::revenu).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> demandes(RapportFilter f) {
        return demandesFiltrees(f).stream().map(this::toDemandeResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RapportPaiementLigne> paiements(RapportFilter f) {
        return paiementRepository.rechercherRapport(f.dateDebut(), f.dateFin(), f.clientId(),
                        f.serviceId(), f.domaineId(), f.commercialId())
                .stream()
                .map(this::toPaiementLigne)
                .toList();
    }

    public RapportData table(String type, RapportFilter f) {
        return switch (type) {
            case "general" -> tableGeneral();
            case "financier" -> tableFinancier(f);
            case "clients" -> tableClients(f);
            case "services" -> tableServices(f);
            case "demandes" -> tableDemandes(f);
            case "paiements" -> tablePaiements(f);
            default -> throw new IllegalArgumentException("Type de rapport inconnu: " + type);
        };
    }

    private RapportData tableGeneral() {
        return new RapportData("Rapport général", List.of("Indicateur", "Valeur"),
                List.of(
                        List.of("Nombre de clients", societeRepository.countByActifTrue()),
                        List.of("Nombre de demandes actives", demandeRepository.countByActifTrue()),
                        List.of("Nombre de services actifs", serviceRepository.countByActifTrue()),
                        List.of("Montant total prévu", format(paiementRepository.sommeTotalePayee() == null ? BigDecimal.ZERO : demandeMontantTotal())),
                        List.of("Montant encaissé", format(paiementRepository.sommeTotalePayee())),
                        List.of("Échéances en retard", echeanceRepository.countEnRetard(LocalDate.now()))
                ));
    }

    private BigDecimal demandeMontantTotal() {
        return demandeRepository.sommeTarifTotal(null, null, null, null, null, null);
    }

    private RapportData tableFinancier(RapportFilter f) {
        RapportFinancierResponse r = financier(f);
        List<List<Object>> lignes = new ArrayList<>();
        lignes.add(List.of("Montant total prévu", r.montantTotalPrevu()));
        lignes.add(List.of("Montant encaissé", r.montantEncaisse()));
        lignes.add(List.of("Reste à payer", r.resteAPayer()));
        lignes.add(List.of("Montant en retard", r.montantEnRetard()));
        for (RapportFinancierMode m : r.paiementsParMode()) {
            lignes.add(List.of("Encaissé - " + m.mode(), m.montant()));
        }
        return new RapportData("Rapport financier", List.of("Indicateur", "Montant"), lignes);
    }

    private RapportData tableClients(RapportFilter f) {
        List<List<Object>> lignes = new ArrayList<>();
        for (RapportClientLigne c : clients(f)) {
            List<Object> row = new ArrayList<>();
            row.add(c.clientId());
            row.add(c.nomSociete());
            row.add(c.nombreDemandes());
            row.add(format(c.montantTotal()));
            row.add(format(c.montantPaye()));
            row.add(format(c.reste()));
            lignes.add(row);
        }
        return new RapportData("Rapport par client", List.of("ID", "Client", "Nb demandes", "Total", "Encaissé", "Reste"), lignes);
    }

    private RapportData tableServices(RapportFilter f) {
        List<List<Object>> lignes = new ArrayList<>();
        for (RapportServiceLigne s : services(f)) {
            List<Object> row = new ArrayList<>();
            row.add(s.serviceId());
            row.add(s.nomService());
            row.add(s.nomDomaine());
            row.add(s.nombreDemandes());
            row.add(format(s.revenu()));
            lignes.add(row);
        }
        return new RapportData("Rapport par service", List.of("ID", "Service", "Domaine", "Nb demandes", "Revenu"), lignes);
    }

    private RapportData tableDemandes(RapportFilter f) {
        List<List<Object>> lignes = new ArrayList<>();
        for (DemandeResponse d : demandes(f)) {
            List<Object> row = new ArrayList<>();
            row.add(d.referenceDemande());
            row.add(d.nomSociete());
            row.add(d.nomService());
            row.add(d.statutActuel());
            row.add(d.dateDemande());
            row.add(format(d.tarif()));
            row.add(format(d.montantPaye()));
            row.add(format(d.resteAPayer()));
            lignes.add(row);
        }
        return new RapportData("Rapport des demandes",
                List.of("Référence", "Client", "Service", "Statut", "Date", "Tarif", "Payé", "Reste"), lignes);
    }

    private RapportData tablePaiements(RapportFilter f) {
        List<List<Object>> lignes = new ArrayList<>();
        for (RapportPaiementLigne p : paiements(f)) {
            List<Object> row = new ArrayList<>();
            row.add(p.referencePaiement());
            row.add(p.referenceDemande());
            row.add(p.nomSociete());
            row.add(p.modePaiement());
            row.add(p.datePaiement());
            row.add(format(p.montant()));
            lignes.add(row);
        }
        return new RapportData("Rapport des paiements",
                List.of("Référence", "Demande", "Client", "Mode", "Date", "Montant"), lignes);
    }

    private List<DemandeService> demandesFiltrees(RapportFilter f) {
        return demandeRepository.rechercher(null, f.clientId(), f.serviceId(), f.domaineId(),
                        f.commercialId(), f.statut(), f.dateDebut(), f.dateFin(), null, Pageable.unpaged())
                .getContent();
    }

    private BigDecimal fin(RapportFilter f) {
        return paiementRepository.sommePayee(f.dateDebut(), f.dateFin(), f.clientId(), f.serviceId(), f.domaineId());
    }

    private BigDecimal fin(RapportFilter f, Integer clientId) {
        return paiementRepository.sommePayee(f.dateDebut(), f.dateFin(), clientId, f.serviceId(), f.domaineId());
    }

    private BigDecimal sommeTarif(List<DemandeService> list) {
        return list.stream().map(DemandeService::getTarif).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DemandeResponse toDemandeResponse(DemandeService d) {
        String statut = historiqueRepository.findFirstByDemandeIdOrderByIdDesc(d.getId())
                .map(h -> h.getStatut().getStatut()).orElse(null);
        BigDecimal paye = paiementRepository.sommePayeeParDemande(d.getId());
        if (paye == null) paye = BigDecimal.ZERO;
        BigDecimal reste = d.getTarif().subtract(paye).max(BigDecimal.ZERO);
        return DemandeMapper.toResponse(d, statut, paye, reste);
    }

    private RapportPaiementLigne toPaiementLigne(Paiement p) {
        return new RapportPaiementLigne(p.getId(), p.getReferencePaiement(),
                p.getDemande() != null ? p.getDemande().getReferenceDemande() : null,
                p.getDemande() != null && p.getDemande().getSociete() != null
                        ? p.getDemande().getSociete().getNomSociete() : null,
                p.getModePaiement() != null ? p.getModePaiement().getLibelle() : null,
                p.getDatePaiement(), p.getMontantPaye());
    }

    private String format(BigDecimal b) {
        return b == null ? "0" : b.stripTrailingZeros().toPlainString();
    }

    private BigDecimal toBigDecimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
