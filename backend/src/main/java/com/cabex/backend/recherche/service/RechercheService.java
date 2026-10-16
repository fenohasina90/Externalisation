package com.cabex.backend.recherche.service;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.recherche.dto.RechercheClientItem;
import com.cabex.backend.recherche.dto.RechercheDemandeItem;
import com.cabex.backend.recherche.dto.RechercheGlobaleResponse;
import com.cabex.backend.recherche.dto.RecherchePaiementItem;
import com.cabex.backend.recherche.dto.RechercheServiceItem;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.repository.ServiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RechercheService {

    private static final int LIMIT_PER_CATEGORIE = 8;

    private final SocieteRepository societeRepository;
    private final ServiceRepository serviceRepository;
    private final DemandeServiceRepository demandeRepository;
    private final PaiementRepository paiementRepository;
    private final HistoriqueDemandeRepository historiqueRepository;

    public RechercheService(SocieteRepository societeRepository,
                            ServiceRepository serviceRepository,
                            DemandeServiceRepository demandeRepository,
                            PaiementRepository paiementRepository,
                            HistoriqueDemandeRepository historiqueRepository) {
        this.societeRepository = societeRepository;
        this.serviceRepository = serviceRepository;
        this.demandeRepository = demandeRepository;
        this.paiementRepository = paiementRepository;
        this.historiqueRepository = historiqueRepository;
    }

    @Transactional(readOnly = true)
    public RechercheGlobaleResponse rechercher(String q) {
        if (q == null || q.isBlank()) {
            return new RechercheGlobaleResponse(List.of(), List.of(), List.of(), List.of());
        }
        String terme = q.trim();
        List<RechercheClientItem> clients = societeRepository
                .rechercherGlobale(terme, PageRequest.of(0, LIMIT_PER_CATEGORIE))
                .stream().map(this::toClientItem).toList();
        List<RechercheServiceItem> services = serviceRepository
                .rechercherGlobale(terme, PageRequest.of(0, LIMIT_PER_CATEGORIE))
                .stream().map(this::toServiceItem).toList();
        List<RechercheDemandeItem> demandes = demandeRepository
                .rechercherGlobale(terme, PageRequest.of(0, LIMIT_PER_CATEGORIE))
                .stream().map(this::toDemandeItem).toList();
        List<RecherchePaiementItem> paiements = "ACCUEIL".equalsIgnoreCase(SecurityUtils.currentRoleName())
                ? List.of()
                : paiementRepository
                .rechercherGlobale(terme, PageRequest.of(0, LIMIT_PER_CATEGORIE))
                .stream().map(this::toPaiementItem).toList();
        return new RechercheGlobaleResponse(clients, services, demandes, paiements);
    }

    private RechercheClientItem toClientItem(Societe s) {
        return new RechercheClientItem(s.getId(), s.getNomSociete(), s.getContact(), s.getEmail());
    }

    private RechercheServiceItem toServiceItem(com.cabex.backend.service.entity.Service s) {
        return new RechercheServiceItem(s.getId(), s.getNomService(), s.getCodeService());
    }

    private RechercheDemandeItem toDemandeItem(DemandeService d) {
        String statut = historiqueRepository.findFirstByDemandeIdOrderByIdDesc(d.getId())
                .map(h -> h.getStatut().getStatut()).orElse(null);
        return new RechercheDemandeItem(d.getId(), d.getReferenceDemande(),
                d.getSociete() != null ? d.getSociete().getNomSociete() : null,
                d.getLignes() != null ? d.getLignes().stream()
                        .map(l -> l.getService() != null ? l.getService().getNomService() : null)
                        .filter(java.util.Objects::nonNull).distinct()
                        .collect(java.util.stream.Collectors.joining(", ")) : null,
                statut, d.getDateDemande(),
                d.getLignes() != null ? d.getLignes().stream()
                        .map(l -> l.getTarif() != null ? l.getTarif() : java.math.BigDecimal.ZERO)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add) : null);
    }

    private RecherchePaiementItem toPaiementItem(Paiement p) {
        return new RecherchePaiementItem(p.getId(), p.getReferencePaiement(),
                p.getDemande() != null ? p.getDemande().getReferenceDemande() : null,
                p.getDemande() != null && p.getDemande().getSociete() != null
                        ? p.getDemande().getSociete().getNomSociete() : null,
                p.getModePaiement() != null ? p.getModePaiement().getLibelle() : null,
                p.getDatePaiement(), p.getMontantPaye());
    }
}
