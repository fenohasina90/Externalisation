package com.cabex.backend.utilisateur.repository;

import com.cabex.backend.utilisateur.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByCodePermission(String codePermission);

    boolean existsByCodePermission(String codePermission);
}
