package co.edu.uniminuto.hyeda.imports.dto;

import co.edu.uniminuto.hyeda.imports.domain.ImportJobStatus;
import co.edu.uniminuto.hyeda.imports.domain.ImportJobType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ImportDtos {

    private ImportDtos() {
    }

    public record ColumnMapping(Map<String, String> fieldToColumn) {
    }

    public record PreviewRequest(ImportJobType tipo, Map<String, String> mapeo) {
    }

    public record PreviewRow(int rowNumber, Map<String, String> values, List<String> errors) {
    }

    public record PreviewResponse(
            ImportJobType jobType,
            String fileName,
            String format,
            List<String> headers,
            Map<String, String> suggestedMapping,
            List<String> requiredFields,
            List<PreviewRow> rows,
            int totalRows,
            int validRows,
            int errorRows
    ) {
    }

    public record ImportJobView(
            Long id,
            ImportJobType jobType,
            String fileName,
            String format,
            ImportJobStatus status,
            int rowsTotal,
            int rowsOk,
            int rowsError,
            String errorLog,
            boolean hasErrorReport,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
    }
}
