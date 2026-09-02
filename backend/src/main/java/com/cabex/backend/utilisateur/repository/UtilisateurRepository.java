package com.cabex.backend.utilisateur.repository;

import com.cabex.backend.utilisateur.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByActifTrue();

    @Modifying
    @Query("UPDATE Utilisateur u SET u.derniereConnexion = :date WHERE u.id = :id")
    void updateDerniereConnexion(@Param("id") Integer id, @Param("date") LocalDateTime date);
}
