package com.cabex.backend.utilisateur.repository;

import com.cabex.backend.utilisateur.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    long countByUtilisateurIdAndUtiliseFalse(Integer utilisateurId);
}
