package com.cabex.backend.service.repository;

import com.cabex.backend.service.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Integer> {

    boolean existsByNomService(String nomService);

    List<Service> findByDomaineId(Integer domaineId);

    List<Service> findByDomaineIdAndActifTrue(Integer domaineId);

    long countByActifTrue();

    @Query("""
        SELECT s FROM Service s
        WHERE (:recherche IS NULL
               OR LOWER(s.nomService) LIKE LOWER(CONCAT('%', :recherche, '%')))
          AND (:domaineId IS NULL OR s.domaine.id = :domaineId)
          AND (:actif IS NULL OR s.actif = :actif)
        """)
    Page<Service> rechercher(@Param("recherche") String recherche,
                             @Param("domaineId") Integer domaineId,
                             @Param("actif") Boolean actif,
                             Pageable pageable);
}
