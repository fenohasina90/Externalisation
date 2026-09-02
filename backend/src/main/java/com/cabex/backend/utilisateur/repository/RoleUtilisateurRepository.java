package com.cabex.backend.utilisateur.repository;

import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleUtilisateurRepository extends JpaRepository<RoleUtilisateur, Integer> {

    Optional<RoleUtilisateur> findByNomRole(String nomRole);

    boolean existsByNomRole(String nomRole);
}
