package com.cabex.backend.client.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.dto.CommercialFicheResponse;
import com.cabex.backend.client.dto.CreateSocieteRequest;
import com.cabex.backend.client.dto.DemandeFicheResponse;
import com.cabex.backend.client.dto.EcheanceFicheResponse;
import com.cabex.backend.client.dto.PaiementFicheResponse;
import com.cabex.backend.client.dto.PieceJointeFicheResponse;
import com.cabex.backend.client.dto.ResponsableResponse;
import com.cabex.backend.client.dto.RisqueClientResponse;
import com.cabex.backend.client.dto.ServiceUtiliseResponse;
import com.cabex.backend.client.dto.SocieteDetailResponse;
import com.cabex.backend.client.dto.SocieteResponse;
import com.cabex.backend.client.dto.SyntheseClientFiche;
import com.cabex.backend.client.dto.UpdateSocieteRequest;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.mapper.ResponsableMapper;
import com.cabex.backend.client.mapper.SocieteMapper;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.document.entity.PieceJointe;
import com.cabex.backend.document.repository.PieceJointeRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.mapper.EcheanceMapper;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SocieteService {

    private static final int JOURS_ALERTE = 3;

    private final SocieteRepository societeRepository;
    private final ClientResponsableRepository responsableRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final PaiementRepository paiementRepository;
    private final EcheancePaiementRepository echeanceRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final DomaineServiceRepository domaineRepository;
    private final RisqueService risqueService;
    private final AuditService auditService;

    public SocieteService(SocieteRepository societeRepository,
                          ClientResponsableRepository responsableRepository,
                          DemandeServiceRepository demandeRepository,
                          HistoriqueDemandeRepository historiqueRepository,
                          PaiementRepository paiementRepository,
                          EcheancePaiementRepository echeanceRepository,
                          PieceJointeRepository pieceJointeRepository,
                          DomaineServiceRepository domaineRepository,
                          RisqueService risqueService,
                          AuditService auditService) {
        this.societeRepository = societeRepository;
        this.responsableRepository = responsableRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueRepository = historiqueRepository;
        this.paiementRepository = paiementRepository;
        this.echeanceRepository = echeanceRepository;
        this.pieceJointeRepository = pieceJointeRepository;
        this.domaineRepository = domaineRepository;
        this.risqueService = risqueService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SocieteResponse> getAll(String recherche, Boolean actif, Integer domaineId, Pageable pageable) {
        Map<Integer, Integer> nbParSociete = new java.util.HashMap<>();
        for (Object[] o : societeRepository.compterDemandesParSociete()) {
            nbParSociete.put((Integer) o[0], ((Number) o[1]).intValue());
        }
        return societeRepository.rechercher(recherche, actif, domaineId, pageable)
                .map(s -> SocieteMapper.toResponse(s, nbParSociete.getOrDefault(s.getId(), 0)));
    }

    @Transactional(readOnly = true)
    public SocieteDetailResponse getDetail(Integer id) {
        Societe societe = getEntity(id);

        List<ResponsableResponse> responsables = responsableRepository.findBySocieteIdAndActifTrue(id).stream()
                .map(ResponsableMapper::toResponse)
                .toList();

        List<DemandeService> demandes = demandeRepository
                .rechercher(null, id, null, null, null, null, null, null, null, Pageable.unpaged()).getContent();

        List<DemandeFicheResponse> demandesFiche = demandes.stream()
                .map(this::toDemandeFiche)
                .toList();

        List<ServiceUtiliseResponse> services = demandes.stream()
                .collect(java.util.stream.Collectors.groupingBy(d -> d.getService().getId()))
                .values().stream()
                .map(list -> new ServiceUtiliseResponse(
                        list.get(0).getService().getId(),
                        list.get(0).getService().getNomService(),
                        societe.getDomaine() != null ? societe.getDomaine().getId() : null,
                        societe.getDomaine() != null ? societe.getDomaine().getNomDomaine() : null,
                        list.get(0).getService().getTarifService(),
                        list.size()))
                .toList();

        boolean accueil = "ACCUEIL".equalsIgnoreCase(SecurityUtils.currentRoleName());

        List<PaiementFicheResponse> paiements = accueil ? List.of()
                : paiementRepository.findBySocieteId(id).stream()
                        .map(p -> new PaiementFicheResponse(
                                p.getId(),
                                p.getReferencePaiement(),
                                p.getDemande() != null ? p.getDemande().getReferenceDemande() : null,
                                p.getDemande() != null ? p.getDemande().getId() : null,
                                p.getModePaiement() != null ? p.getModePaiement().getLibelle() : null,
                                p.getDatePaiement(),
                                p.getMontantPaye(),
                                p.getReferenceVirement()))
                        .toList();

        LocalDate today = LocalDate.now();
        List<EcheanceFicheResponse> echeances = accueil ? List.of()
                : echeanceRepository.findBySocieteId(id).stream()
                        .map(e -> new EcheanceFicheResponse(
                                e.getId(),
                                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                                        ? e.getProposPaiement().getDemande().getReferenceDemande() : null,
                                e.getProposPaiement() != null && e.getProposPaiement().getDemande() != null
                                        ? e.getProposPaiement().getDemande().getId() : null,
                                e.getNumeroEcheance(),
                                e.getDateEcheance(),
                                e.getMontantAttendu(),
                                e.getMontantPaye(),
                                e.getMontantAttendu().subtract(e.getMontantPaye()),
                                EcheanceMapper.calculerStatutEcheance(e, today, JOURS_ALERTE)))
                        .toList();

        List<CommercialFicheResponse> commerciaux = commerciauxDuClient(demandes);

        List<PieceJointeFicheResponse> piecesJointes = pieceJointeRepository
                .findByDemandeSocieteId(id).stream()
                .map(this::toPieceJointeFiche)
                .toList();

        SyntheseClientFiche synthese = accueil ? null
                : syntheseDuClient(demandes, paiements, echeances, id);

        return new SocieteDetailResponse(
                SocieteMapper.toResponse(societe, demandes.size()),
                responsables,
                commerciaux,
                demandesFiche,
                services,
                paiements,
                echeances,
                piecesJointes,
                synthese);
    }

    private List<CommercialFicheResponse> commerciauxDuClient(List<DemandeService> demandes) {
        Map<Integer, Commercial> distincts = new LinkedHashMap<>();
        for (DemandeService d : demandes) {
            if (d.getCommercial() != null) {
                distincts.putIfAbsent(d.getCommercial().getId(), d.getCommercial());
            }
        }
        return distincts.values().stream()
                .map(c -> new CommercialFicheResponse(
                        c.getId(), c.getNomCommercial(), c.getImmatricule(), c.getEmail(), c.getContact()))
                .toList();
    }

    private PieceJointeFicheResponse toPieceJointeFiche(PieceJointe p) {
        return new PieceJointeFicheResponse(
                p.getId(),
                p.getNomOriginal(),
                p.getTypeFichier(),
                p.getTailleFichier(),
                p.getDescription(),
                p.getDemande() != null ? p.getDemande().getId() : null,
                p.getDemande() != null ? p.getDemande().getReferenceDemande() : null,
                p.getCreatedAt());
    }

    private SyntheseClientFiche syntheseDuClient(List<DemandeService> demandes,
                                                 List<PaiementFicheResponse> paiements,
                                                 List<EcheanceFicheResponse> echeances,
                                                 Integer societeId) {
        BigDecimal montantTotal = demandes.stream()
                .map(d -> d.getTarifServiceApplique() == null ? BigDecimal.ZERO : d.getTarifServiceApplique())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montantPaye = paiements.stream()
                .map(PaiementFicheResponse::montantPaye)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal resteAPayer = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);

        long enRetard = echeances.stream()
                .filter(e -> "EN_RETARD".equals(e.statut()))
                .count();

        RisqueClientResponse risque = risqueService.calculer(societeId);
        return new SyntheseClientFiche(
                montantTotal, montantPaye, resteAPayer, enRetard, risque.tauxPaiementATemps(), risque.risque());
    }

    @Transactional
    public SocieteResponse create(CreateSocieteRequest request) {
        if (societeRepository.existsByNomSociete(request.nomSociete().trim())) {
            throw new DuplicateResourceException(
                    ErrorMessages.referenceDejaUtilisee(request.nomSociete().trim()));
        }
        Societe societe = Societe.builder()
                .nomSociete(request.nomSociete().trim())
                .activite(request.activite())
                .contact(request.contact())
                .email(request.email())
                .fb(request.fb())
                .adresse(request.adresse())
                .dateCreation(request.dateCreation())
                .nombrePersonnel(request.nombrePersonnel())
                .chiffreAffaireMensuel(request.chiffreAffaireMensuel())
                .domaine(getDomaine(request.domaineId()))
                .actif(true)
                .build();
        Societe saved = societeRepository.save(societe);
        auditService.log(AuditActions.CREATE, "CLIENT", "Societe", saved.getId().longValue(), null, "nom=" + saved.getNomSociete());
        return SocieteMapper.toResponse(saved);
    }

    @Transactional
    public SocieteResponse update(Integer id, UpdateSocieteRequest request) {
        Societe societe = getEntity(id);
        societe.setNomSociete(request.nomSociete().trim());
        societe.setActivite(request.activite());
        societe.setContact(request.contact());
        societe.setEmail(request.email());
        societe.setFb(request.fb());
        societe.setAdresse(request.adresse());
        societe.setDateCreation(request.dateCreation());
        societe.setNombrePersonnel(request.nombrePersonnel());
        societe.setChiffreAffaireMensuel(request.chiffreAffaireMensuel());
        societe.setDomaine(getDomaine(request.domaineId()));
        Societe saved = societeRepository.save(societe);
        auditService.log(AuditActions.UPDATE, "CLIENT", "Societe", saved.getId().longValue(), null, "nom=" + saved.getNomSociete());
        return SocieteMapper.toResponse(saved);
    }

    @Transactional
    public SocieteResponse archive(Integer id, boolean archive) {
        Societe societe = getEntity(id);
        societe.setActif(!archive);
        societe.setArchivedAt(archive ? LocalDateTime.now() : null);
        Societe saved = societeRepository.save(societe);
        auditService.log(archive ? AuditActions.ARCHIVE : AuditActions.UPDATE, "CLIENT", "Societe", saved.getId().longValue(), "actif=" + !archive, "actif=" + saved.getActif());
        return SocieteMapper.toResponse(saved);
    }

    private DemandeFicheResponse toDemandeFiche(DemandeService d) {
        BigDecimal montantPaye = paiementRepository.sommePayeeParDemande(d.getId());
        BigDecimal reste = d.getTarifServiceApplique().subtract(montantPaye);
        String statut = deriverStatut(d.getId());
        return new DemandeFicheResponse(
                d.getId(),
                d.getReferenceDemande(),
                d.getDateDemande(),
                d.getDebutService(),
                d.getFinService(),
                d.getService() != null ? d.getService().getNomService() : null,
                statut,
                montantPaye,
                reste,
                d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : null);
    }

    private String deriverStatut(Integer demandeId) {
        List<HistoriqueDemande> historiques = historiqueRepository
                .findByDemandeIdOrderByCreatedAtAsc(demandeId);
        if (historiques.isEmpty()) {
            return "EN_ATTENTE";
        }
        HistoriqueDemande dernier = historiques.get(historiques.size() - 1);
        return dernier.getStatut() != null ? dernier.getStatut().getStatut() : "EN_ATTENTE";
    }

    private DomaineService getDomaine(Integer id) {
        return domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Domaine", id)));
    }

    private Societe getEntity(Integer id) {
        return societeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Société", id)));
    }
}
