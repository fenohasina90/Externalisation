package com.cabex.backend.dossier.repository;

import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DossierTransactionImmobiliereRepository
        extends JpaRepository<DossierTransactionImmobiliere, Integer> {

    Optional<DossierTransactionImmobiliere> findByDemandeId(Integer demandeId);

    boolean existsByDemandeId(Integer demandeId);
}
