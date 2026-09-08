package co.edu.uniminuto.hyeda.imports.service;

import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee CSV o Excel y lo deja como filas de texto, sin interpretar el negocio.
 */
@Component
public class SpreadsheetParser {

    public record SheetData(String format, List<String> headers, List<Map<String, String>> rows) {
    }

    public SheetData parse(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) {
                return parseCsv(file);
            }
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseExcel(file);
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo: " + e.getMessage());
        }
        throw new BusinessException("Usa un archivo CSV o Excel (.xlsx)");
    }

    private SheetData parseCsv(MultipartFile file) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();
        try (CSVParser parser = format.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> headers = new ArrayList<>(parser.getHeaderNames());
            if (headers.isEmpty()) {
                throw new BusinessException("El CSV no tiene encabezados");
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                boolean empty = true;
                for (String header : headers) {
                    String value = record.isMapped(header) ? record.get(header) : "";
                    row.put(header, value == null ? "" : value.trim());
                    if (value != null && !value.isBlank()) {
                        empty = false;
                    }
                }
                if (!empty) {
                    rows.add(row);
                }
            }
            return new SheetData("CSV", headers, rows);
        }
    }

    private SheetData parseExcel(MultipartFile file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new BusinessException("El Excel esta vacio");
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new BusinessException("El Excel no tiene encabezados");
            }
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = formatter.formatCellValue(headerRow.getCell(i)).trim();
                headers.add(header.isBlank() ? "columna_" + (i + 1) : header);
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row excelRow = sheet.getRow(r);
                if (excelRow == null) {
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                boolean empty = true;
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = excelRow.getCell(i);
                    String value = formatter.formatCellValue(cell).trim();
                    row.put(headers.get(i), value);
                    if (!value.isBlank()) {
                        empty = false;
                    }
                }
                if (!empty) {
                    rows.add(row);
                }
            }
            return new SheetData("XLSX", headers, rows);
        }
    }
}
