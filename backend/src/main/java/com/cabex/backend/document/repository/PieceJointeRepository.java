package com.cabex.backend.document.repository;

import com.cabex.backend.document.entity.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PieceJointeRepository extends JpaRepository<PieceJointe, Integer> {

    List<PieceJointe> findByDemandeIdOrderByCreatedAtDesc(Integer demandeId);
}
