package com.cabex.backend.service.repository;

import com.cabex.backend.service.entity.HistoriqueTarifService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoriqueTarifServiceRepository extends JpaRepository<HistoriqueTarifService, Integer> {

    List<HistoriqueTarifService> findByServiceIdOrderByDateDebutDesc(Integer serviceId);

    Optional<HistoriqueTarifService> findFirstByServiceIdOrderByDateDebutDesc(Integer serviceId);

    boolean existsByServiceIdAndDateFinIsNull(Integer serviceId);
}
