package com.cabex.backend.notification.repository;

import com.cabex.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUtilisateurIdOrderByDateCreationDesc(Integer utilisateurId, Pageable pageable);

    Page<Notification> findByUtilisateurIdAndTypeNotificationInOrderByDateCreationDesc(
            Integer utilisateurId, Collection<String> types, Pageable pageable);

    Page<Notification> findByUtilisateurIdAndLuFalseOrderByDateCreationDesc(Integer utilisateurId, Pageable pageable);

    Page<Notification> findByUtilisateurIdAndLuFalseAndTypeNotificationInOrderByDateCreationDesc(
            Integer utilisateurId, Collection<String> types, Pageable pageable);

    long countByUtilisateurIdAndLuFalse(Integer utilisateurId);

    long countByUtilisateurIdAndLuFalseAndTypeNotificationIn(Integer utilisateurId, Collection<String> types);

    boolean existsByUtilisateurIdAndTypeNotificationAndReferenceIdAndLuFalse(
            Integer utilisateurId, String typeNotification, Long referenceId);

    @Modifying
    @Query("UPDATE Notification n SET n.lu = true, n.dateLecture = :date WHERE n.utilisateur.id = :userId AND n.lu = false")
    int marquerToutLu(@Param("userId") Integer userId, @Param("date") LocalDateTime date);

    @Modifying
    @Query("UPDATE Notification n SET n.lu = true, n.dateLecture = :date "
            + "WHERE n.utilisateur.id = :userId AND n.lu = false AND n.typeNotification IN :types")
    int marquerToutLuDansTypes(@Param("userId") Integer userId,
                               @Param("types") Collection<String> types,
                               @Param("date") LocalDateTime date);
}
