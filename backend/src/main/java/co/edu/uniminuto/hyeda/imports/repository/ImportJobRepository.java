package co.edu.uniminuto.hyeda.imports.repository;

import co.edu.uniminuto.hyeda.imports.domain.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    List<ImportJob> findAllByOrderByStartedAtDesc();
}
