package com.cabex.backend.commercial.repository;

import com.cabex.backend.commercial.entity.Commercial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommercialRepository extends JpaRepository<Commercial, Integer> {

    Optional<Commercial> findByImmatricule(String immatricule);

    boolean existsByImmatricule(String immatricule);

    boolean existsByEmail(String email);
}
