package com.cabex.backend.utilisateur.repository;

import com.cabex.backend.utilisateur.entity.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByEmail(String email);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<Utilisateur> findByUsername(String username);

    List<Utilisateur> findByRoleNomRoleAndActifTrue(String nomRole);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByActifTrue();

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:roleId IS NULL OR u.role.id = :roleId)
              AND (:actif IS NULL OR u.actif = :actif)
            """)
    Page<Utilisateur> findWithFilters(@Param("search") String search,
                                      @Param("roleId") Integer roleId,
                                      @Param("actif") Boolean actif,
                                      Pageable pageable);

    @Modifying
    @Query("UPDATE Utilisateur u SET u.derniereConnexion = :date WHERE u.id = :id")
    void updateDerniereConnexion(@Param("id") Integer id, @Param("date") LocalDateTime date);
}
