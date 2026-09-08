package co.edu.uniminuto.hyeda.imports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_job")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private ImportJobType jobType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(nullable = false, length = 10)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ImportJobStatus status = ImportJobStatus.PENDIENTE;

    @Column(name = "rows_total", nullable = false)
    @Builder.Default
    private Integer rowsTotal = 0;

    @Column(name = "rows_ok", nullable = false)
    @Builder.Default
    private Integer rowsOk = 0;

    @Column(name = "rows_error", nullable = false)
    @Builder.Default
    private Integer rowsError = 0;

    @Column(name = "error_log", length = 4000)
    private String errorLog;

    @Column(name = "started_by")
    private Long startedBy;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
