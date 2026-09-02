package com.cabex.backend.audit.repository;

import com.cabex.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:utilisateurId IS NULL OR a.utilisateur.id = :utilisateurId)
          AND (:action IS NULL OR a.action = :action)
          AND (:module IS NULL OR a.module = :module)
          AND (:entite IS NULL OR a.entite = :entite)
          AND (:dateDebut IS NULL OR a.createdAt >= :dateDebut)
          AND (:dateFin IS NULL OR a.createdAt <= :dateFin)
        """)
    Page<AuditLog> rechercher(@Param("utilisateurId") Integer utilisateurId,
                              @Param("action") String action,
                              @Param("module") String module,
                              @Param("entite") String entite,
                              @Param("dateDebut") LocalDateTime dateDebut,
                              @Param("dateFin") LocalDateTime dateFin,
                              Pageable pageable);
}
