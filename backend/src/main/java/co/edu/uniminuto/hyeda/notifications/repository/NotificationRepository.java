package co.edu.uniminuto.hyeda.notifications.repository;

import co.edu.uniminuto.hyeda.notifications.domain.Notification;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationStatus;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Cola de salida: lo mas viejo primero, sin pasarse de reintentos. */
    @Query("""
            select n from Notification n
            where n.status = 'EN_COLA' and n.attempts < :maxAttempts
            order by n.createdAt asc
            """)
    List<Notification> findPending(@Param("maxAttempts") int maxAttempts, Pageable pageable);

    @Query("""
            select n from Notification n
            where (:status is null or n.status = :status)
              and (:template is null or n.templateCode = :template)
            order by n.createdAt desc
            """)
    Page<Notification> search(@Param("status") NotificationStatus status,
                              @Param("template") NotificationTemplate template,
                              Pageable pageable);

    boolean existsByRentalIdAndTemplateCodeAndStatusIn(Long rentalId, NotificationTemplate template,
                                                       List<NotificationStatus> statuses);

    boolean existsByCustomerIdAndTemplateCodeAndCreatedAtAfter(Long customerId,
                                                               NotificationTemplate template,
                                                               LocalDateTime after);

    long countByStatus(NotificationStatus status);
}
