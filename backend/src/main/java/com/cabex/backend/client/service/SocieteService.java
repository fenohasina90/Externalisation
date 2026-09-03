package com.cabex.backend.client.service;

import com.cabex.backend.client.dto.CreateSocieteRequest;
import com.cabex.backend.client.dto.DemandeFicheResponse;
import com.cabex.backend.client.dto.EcheanceFicheResponse;
import com.cabex.backend.client.dto.PaiementFicheResponse;
import com.cabex.backend.client.dto.ResponsableResponse;
import com.cabex.backend.client.dto.ServiceUtiliseResponse;
import com.cabex.backend.client.dto.SocieteDetailResponse;
import com.cabex.backend.client.dto.SocieteResponse;
import com.cabex.backend.client.dto.UpdateSocieteRequest;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.mapper.ResponsableMapper;
import com.cabex.backend.client.mapper.SocieteMapper;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.mapper.EcheanceMapper;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.PaiementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SocieteService {

    private static final int JOURS_ALERTE = 3;

    private final SocieteRepository societeRepository;
    private final ClientResponsableRepository responsableRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final PaiementRepository paiementRepository;
    private final EcheancePaiementRepository echeanceRepository;

    public SocieteService(SocieteRepository societeRepository,
                          ClientResponsableRepository responsableRepository,
                          DemandeServiceRepository demandeRepository,
                          HistoriqueDemandeRepository historiqueRepository,
                          PaiementRepository paiementRepository,
                          EcheancePaiementRepository echeanceRepository) {
        this.societeRepository = societeRepository;
        this.responsableRepository = responsableRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueRepository = historiqueRepository;
        this.paiementRepository = paiementRepository;
        this.echeanceRepository = echeanceRepository;
    }

    @Transactional(readOnly = true)
    public Page<SocieteResponse> getAll(String recherche, Boolean actif, Pageable pageable) {
        return societeRepository.rechercher(recherche, actif, pageable)
                .map(SocieteMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SocieteDetailResponse getDetail(Integer id) {
        Societe societe = getEntity(id);

        List<ResponsableResponse> responsables = responsableRepository.findBySocieteIdAndActifTrue(id).stream()
                .map(ResponsableMapper::toResponse)
                .toList();

        List<DemandeService> demandes = demandeRepository
                .rechercher(null, id, null, null, null, null, null, Pageable.unpaged()).getContent();

        List<DemandeFicheResponse> demandesFiche = demandes.stream()
                .map(this::toDemandeFiche)
                .toList();

        List<ServiceUtiliseResponse> services = demandes.stream()
                .collect(java.util.stream.Collectors.groupingBy(d -> d.getService().getId()))
                .values().stream()
                .map(list -> new ServiceUtiliseResponse(
                        list.get(0).getService().getId(),
                        list.get(0).getService().getNomService(),
                        list.get(0).getService().getDomaine() != null
                                ? list.get(0).getService().getDomaine().getId() : null,
                        list.get(0).getService().getDomaine() != null
                                ? list.get(0).getService().getDomaine().getNomDomaine() : null,
                        list.get(0).getService().getTarifService(),
                        list.size()))
                .toList();

        List<PaiementFicheResponse> paiements = paiementRepository.findBySocieteId(id).stream()
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
        List<EcheanceFicheResponse> echeances = echeanceRepository.findBySocieteId(id).stream()
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

        return new SocieteDetailResponse(
                SocieteMapper.toResponse(societe, demandes.size()),
                responsables,
                demandesFiche,
                services,
                paiements,
                echeances);
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
                .actif(true)
                .build();
        return SocieteMapper.toResponse(societeRepository.save(societe));
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
        return SocieteMapper.toResponse(societeRepository.save(societe));
    }

    @Transactional
    public SocieteResponse archive(Integer id, boolean archive) {
        Societe societe = getEntity(id);
        societe.setActif(!archive);
        societe.setArchivedAt(archive ? LocalDateTime.now() : null);
        return SocieteMapper.toResponse(societeRepository.save(societe));
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

    private Societe getEntity(Integer id) {
        return societeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Société", id)));
    }
}
