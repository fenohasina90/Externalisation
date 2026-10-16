package com.cabex.backend.echeance.repository;

import com.cabex.backend.echeance.entity.ProposPaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProposPaiementRepository extends JpaRepository<ProposPaiement, Integer> {

    List<ProposPaiement> findByDemandeIdOrderByCreatedAtDesc(Integer demandeId);

    Optional<ProposPaiement> findFirstByDemandeIdAndStatutOrderByCreatedAtDesc(Integer demandeId, String statut);

    boolean existsByDemandeId(Integer demandeId);

    List<ProposPaiement> findByLigneServiceId(Integer ligneServiceId);

    Optional<ProposPaiement> findFirstByLigneServiceIdAndStatutOrderByCreatedAtDesc(
            Integer ligneServiceId, String statut);

    boolean existsByLigneServiceId(Integer ligneServiceId);
}
