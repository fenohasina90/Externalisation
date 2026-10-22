package com.cabex.backend.demande.repository;

import com.cabex.backend.demande.entity.HistoriqueDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HistoriqueDemandeRepository extends JpaRepository<HistoriqueDemande, Integer> {

    List<HistoriqueDemande> findByDemandeIdOrderByCreatedAtAsc(Integer demandeId);

    List<HistoriqueDemande> findByDemandeIdOrderByIdAsc(Integer demandeId);

    Optional<HistoriqueDemande> findFirstByDemandeIdOrderByCreatedAtDesc(Integer demandeId);

    Optional<HistoriqueDemande> findFirstByDemandeIdOrderByIdDesc(Integer demandeId);

    long countByDemandeId(Integer demandeId);

    /** Statut courant (dernier historique) pour un lot de demandes. */
    @Query("""
        SELECT h.demande.id, h.statut.statut
        FROM HistoriqueDemande h
        WHERE h.demande.id IN :demandeIds
          AND h.id = (SELECT MAX(h2.id) FROM HistoriqueDemande h2
                      WHERE h2.demande.id = h.demande.id)
        """)
    List<Object[]> statutsActuels(@Param("demandeIds") Collection<Integer> demandeIds);

    @Query("""
        SELECT h.demande.id, h.demande.dateDemande, MIN(h.createdAt)
        FROM HistoriqueDemande h
        WHERE h.statut.statut = 'TERMINEE'
        GROUP BY h.demande.id, h.demande.dateDemande
        """)
    List<Object[]> termeineesEtDates();

    @Query("""
        SELECT h.demande.id, h.demande.dateDemande, MIN(h.createdAt)
        FROM HistoriqueDemande h
        LEFT JOIN h.demande.societe s
        WHERE h.statut.statut = 'TERMINEE'
          AND h.demande.actif = true
          AND (:dateDebut IS NULL OR h.demande.dateDemande >= :dateDebut)
          AND (:dateFin IS NULL OR h.demande.dateDemande <= :dateFin)
          AND (:societeId IS NULL OR s.id = :societeId)
          AND (:serviceId IS NULL OR EXISTS (
                SELECT 1 FROM LigneService l
                WHERE l.demande = h.demande AND l.service.id = :serviceId))
        GROUP BY h.demande.id, h.demande.dateDemande
        """)
    List<Object[]> termeineesEtDatesFiltrees(@Param("dateDebut") LocalDate dateDebut,
                                             @Param("dateFin") LocalDate dateFin,
                                             @Param("societeId") Integer societeId,
                                             @Param("serviceId") Integer serviceId);
}
