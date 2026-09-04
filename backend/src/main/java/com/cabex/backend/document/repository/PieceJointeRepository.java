package com.cabex.backend.document.repository;

import com.cabex.backend.document.entity.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PieceJointeRepository extends JpaRepository<PieceJointe, Integer> {

    List<PieceJointe> findByDemandeIdOrderByCreatedAtDesc(Integer demandeId);

    @Query("""
        SELECT p FROM PieceJointe p
        WHERE p.demande.societe.id = :societeId
        ORDER BY p.createdAt DESC
        """)
    List<PieceJointe> findByDemandeSocieteId(@Param("societeId") Integer societeId);
}
