package com.cabex.backend.client.service;

import com.cabex.backend.client.dto.RisqueClientResponse;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class RisqueService {

    private final SocieteRepository societeRepository;
    private final EcheancePaiementRepository echeanceRepository;

    public RisqueService(SocieteRepository societeRepository, EcheancePaiementRepository echeanceRepository) {
        this.societeRepository = societeRepository;
        this.echeanceRepository = echeanceRepository;
    }

    @Transactional(readOnly = true)
    public RisqueClientResponse calculer(Integer societeId) {
        if (societeRepository.findById(societeId).isEmpty()) {
            throw new ResourceNotFoundException(ErrorMessages.introuvable("Société", societeId));
        }
        LocalDate today = LocalDate.now();
        List<EcheancePaiement> echeances = echeanceRepository.findBySocieteId(societeId);

        BigDecimal totalAttendu = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;
        BigDecimal enRetard = BigDecimal.ZERO;
        long nbEnRetard = 0;

        for (EcheancePaiement e : echeances) {
            BigDecimal attendu = e.getMontantAttendu() == null ? BigDecimal.ZERO : e.getMontantAttendu();
            BigDecimal paye = e.getMontantPaye() == null ? BigDecimal.ZERO : e.getMontantPaye();
            totalAttendu = totalAttendu.add(attendu);
            totalPaye = totalPaye.add(paye);
            BigDecimal reste = attendu.subtract(paye);
            if (e.getDateEcheance() != null && e.getDateEcheance().isBefore(today) && reste.compareTo(BigDecimal.ZERO) > 0) {
                enRetard = enRetard.add(reste);
                nbEnRetard++;
            }
        }

        BigDecimal tauxPaiement;
        if (totalAttendu.compareTo(BigDecimal.ZERO) == 0) {
            tauxPaiement = BigDecimal.ZERO;
        } else {
            tauxPaiement = totalPaye.multiply(BigDecimal.valueOf(100))
                    .divide(totalAttendu, 0, RoundingMode.HALF_UP);
        }

        String risque;
        String note;
        if (totalAttendu.compareTo(BigDecimal.ZERO) == 0) {
            risque = "FAIBLE";
            note = "Aucune échéance de paiement enregistrée pour ce client.";
        } else {
            BigDecimal ratioEnRetard = enRetard.multiply(BigDecimal.valueOf(100))
                    .divide(totalAttendu, 0, RoundingMode.HALF_UP);
            if (ratioEnRetard.compareTo(BigDecimal.valueOf(50)) >= 0) {
                risque = "ELEVE";
                note = "Plus de la moitié du montant dû est en retard.";
            } else if (nbEnRetard > 0 && ratioEnRetard.compareTo(BigDecimal.ZERO) > 0) {
                risque = "MOYEN";
                note = "Présence d'échéances en retard mais inférieures à la moitié du montant dû.";
            } else if (tauxPaiement.compareTo(BigDecimal.valueOf(50)) < 0) {
                risque = "MOYEN";
                note = "Taux de paiement faible malgré l'absence de retard.";
            } else {
                risque = "FAIBLE";
                note = "Aucun retard et paiements réguliers.";
            }
        }

        return new RisqueClientResponse(risque, note, totalAttendu, totalPaye, enRetard, nbEnRetard, tauxPaiement);
    }
}
