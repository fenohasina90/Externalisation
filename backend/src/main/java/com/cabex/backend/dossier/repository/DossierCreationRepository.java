package com.cabex.backend.dossier.repository;

import com.cabex.backend.dossier.entity.DossierCreation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DossierCreationRepository extends JpaRepository<DossierCreation, Integer> {

    Optional<DossierCreation> findByDemandeId(Integer demandeId);

    boolean existsByDemandeId(Integer demandeId);
}
