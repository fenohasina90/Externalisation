package com.cabex.backend.client.repository;

import com.cabex.backend.client.entity.ClientResponsable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientResponsableRepository extends JpaRepository<ClientResponsable, Integer> {

    List<ClientResponsable> findBySocieteId(Integer societeId);

    List<ClientResponsable> findBySocieteIdAndActifTrue(Integer societeId);

    long countBySocieteId(Integer societeId);
}
