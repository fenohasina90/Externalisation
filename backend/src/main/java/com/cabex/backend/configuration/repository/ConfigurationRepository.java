package com.cabex.backend.configuration.repository;

import com.cabex.backend.configuration.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigurationRepository extends JpaRepository<Configuration, Integer> {

    Optional<Configuration> findByCle(String cle);

    boolean existsByCle(String cle);
}
