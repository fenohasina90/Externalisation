package com.cabex.backend.paiement.repository;

import com.cabex.backend.paiement.entity.ModePaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModePaiementRepository extends JpaRepository<ModePaiement, Integer> {

    Optional<ModePaiement> findByCode(String code);

    boolean existsByCode(String code);
}
