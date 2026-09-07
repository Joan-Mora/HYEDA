package co.edu.uniminuto.hyeda.modules.integraciones.repository;

import co.edu.uniminuto.hyeda.modules.integraciones.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatus(String status);
}
