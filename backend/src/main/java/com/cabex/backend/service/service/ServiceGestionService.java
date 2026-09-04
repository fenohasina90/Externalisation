package com.cabex.backend.service.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.dto.CreateServiceRequest;
import com.cabex.backend.service.dto.HistoriqueTarifResponse;
import com.cabex.backend.service.dto.ServiceResponse;
import com.cabex.backend.service.dto.UpdateServiceRequest;
import com.cabex.backend.service.dto.UpdateTarifRequest;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.entity.HistoriqueTarifService;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.mapper.HistoriqueTarifMapper;
import com.cabex.backend.service.mapper.ServiceMapper;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import com.cabex.backend.service.repository.HistoriqueTarifServiceRepository;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Service
public class ServiceGestionService {

    private final ServiceRepository serviceRepository;
    private final DomaineServiceRepository domaineRepository;
    private final HistoriqueTarifServiceRepository historiqueRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    public ServiceGestionService(ServiceRepository serviceRepository,
                          DomaineServiceRepository domaineRepository,
                          HistoriqueTarifServiceRepository historiqueRepository,
                          UtilisateurRepository utilisateurRepository,
                          AuditService auditService) {
        this.serviceRepository = serviceRepository;
        this.domaineRepository = domaineRepository;
        this.historiqueRepository = historiqueRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getAll(String recherche, Integer domaineId, Boolean actif, Pageable pageable) {
        return serviceRepository.rechercher(recherche, domaineId, actif, pageable)
                .map(ServiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getById(Integer id) {
        return ServiceMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
        if (serviceRepository.existsByNomService(request.nomService().trim())) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(request.nomService().trim()));
        }
        DomaineService domaine = domaineRepository.findById(request.domaineId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Domaine", request.domaineId())));

        Service service = Service.builder()
                .nomService(request.nomService().trim())
                .tarifService(request.tarifService())
                .description(request.description())
                .domaine(domaine)
                .actif(true)
                .build();
        service = serviceRepository.save(service);

        auditService.log(AuditActions.CREATE, "SERVICE", "Service", service.getId().longValue(), null, "nom=" + service.getNomService());

        HistoriqueTarifService historique = HistoriqueTarifService.builder()
                .service(service)
                .montant(request.tarifService())
                .dateDebut(LocalDateTime.now())
                .dateFin(null)
                .utilisateur(currentUtilisateur())
                .build();
        historiqueRepository.save(historique);

        return ServiceMapper.toResponse(service);
    }

    @Transactional
    public ServiceResponse update(Integer id, UpdateServiceRequest request) {
        Service service = getEntity(id);
        service.setNomService(request.nomService().trim());
        service.setDescription(request.description());
        Service saved = serviceRepository.save(service);
        auditService.log(AuditActions.UPDATE, "SERVICE", "Service", saved.getId().longValue(), null, "nom=" + saved.getNomService());
        return ServiceMapper.toResponse(saved);
    }

    @Transactional
    public ServiceResponse archive(Integer id, boolean archive) {
        Service service = getEntity(id);
        boolean previousActif = service.getActif();
        service.setActif(!archive);
        service.setArchivedAt(archive ? LocalDateTime.now() : null);
        Service saved = serviceRepository.save(service);
        auditService.log(archive ? AuditActions.ARCHIVE : AuditActions.UPDATE, "SERVICE", "Service", saved.getId().longValue(), "actif=" + previousActif, "actif=" + saved.getActif());
        return ServiceMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueTarifResponse> getHistoriqueTarifs(Integer serviceId) {
        getEntity(serviceId);
        return historiqueRepository.findByServiceIdOrderByDateDebutDesc(serviceId).stream()
                .map(HistoriqueTarifMapper::toResponse)
                .toList();
    }

    @Transactional
    public HistoriqueTarifResponse updateTarif(Integer serviceId, UpdateTarifRequest request) {
        Service service = getEntity(serviceId);
        String ancienTarif = "tarif=" + service.getTarifService();

        // Fermer le tarif courant actif s'il existe
        historiqueRepository.findFirstByServiceIdOrderByDateDebutDesc(serviceId)
                .filter(h -> h.getDateFin() == null)
                .ifPresent(h -> {
                    h.setDateFin(LocalDateTime.now());
                    historiqueRepository.save(h);
                });

        // Nouveau tarif courant
        HistoriqueTarifService nouveau = HistoriqueTarifService.builder()
                .service(service)
                .montant(request.montant())
                .dateDebut(LocalDateTime.now())
                .dateFin(null)
                .utilisateur(currentUtilisateur())
                .build();
        nouveau = historiqueRepository.save(nouveau);

        service.setTarifService(request.montant());
        serviceRepository.save(service);

        auditService.log(AuditActions.UPDATE, "SERVICE", "Tarif", service.getId().longValue(), ancienTarif, "tarif=" + request.montant());

        return HistoriqueTarifMapper.toResponse(nouveau);
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Service getEntity(Integer id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Service", id)));
    }
}
