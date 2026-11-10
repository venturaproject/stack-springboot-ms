package com.boilerplate.domain.notifications;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Persistence port for {@link Notification}. */
public interface NotificationRepository extends Repository<Notification, UUID> {

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId OR n.userId IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findVisibleTo(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.userId = :userId OR n.userId IS NULL) AND n.read = false")
    long countUnreadFor(@Param("userId") UUID userId);

    Notification save(Notification notification);

    void delete(Notification notification);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    void deleteAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE (n.userId = :userId OR n.userId IS NULL) AND n.read = false")
    void markAllReadFor(@Param("userId") UUID userId);
}
