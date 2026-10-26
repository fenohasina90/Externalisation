package com.cabex.backend.dossier.repository;

import com.cabex.backend.dossier.entity.DossierAssocie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DossierAssocieRepository extends JpaRepository<DossierAssocie, Integer> {

    List<DossierAssocie> findByDossierIdOrderByOrdreAsc(Integer dossierId);

    void deleteByDossierId(Integer dossierId);
}
