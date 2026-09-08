package co.edu.uniminuto.hyeda.imports.service;

import co.edu.uniminuto.hyeda.imports.domain.ImportJobType;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Nombres de columna que la administradora suele usar en sus Excel.
 */
final class ColumnAliases {

    private ColumnAliases() {
    }

    static List<String> requiredFields(ImportJobType type) {
        return switch (type) {
            case PRODUCTOS -> List.of("name", "dailyPrice", "stockTotal");
            case CLIENTES -> List.of("document", "name");
            case MOVIMIENTOS -> List.of("movementType", "amount", "movementDate");
            case DEUDAS -> List.of("document", "originalAmount");
        };
    }

    static List<String> allFields(ImportJobType type) {
        return switch (type) {
            case PRODUCTOS -> List.of("sku", "name", "description", "category", "unit",
                    "dailyPrice", "lateFeePerDay", "replacementValue", "stockTotal", "minStockThreshold");
            case CLIENTES -> List.of("document", "documentType", "name", "phone", "email",
                    "notes", "address", "reference");
            case MOVIMIENTOS -> List.of("movementType", "amount", "description", "category", "movementDate");
            case DEUDAS -> List.of("document", "customerName", "originalAmount", "balance", "dueDate");
        };
    }

    static Map<String, String> suggest(ImportJobType type, List<String> headers) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String field : allFields(type)) {
            headers.stream()
                    .filter(header -> aliases(field).contains(normalize(header)))
                    .findFirst()
                    .ifPresent(header -> mapping.put(field, header));
        }
        return mapping;
    }

    private static Set<String> aliases(String field) {
        return switch (field) {
            case "sku" -> Set.of("sku", "codigo", "cod", "referencia", "ref");
            case "name" -> Set.of("nombre", "name", "producto", "equipo", "descripcion_corta");
            case "description" -> Set.of("descripcion", "description", "detalle");
            case "category" -> Set.of("categoria", "category", "grupo", "familia");
            case "unit" -> Set.of("unidad", "unit", "und");
            case "dailyPrice" -> Set.of("precio", "precio_dia", "precio/dia", "valor_dia", "dailyprice", "tarifa");
            case "lateFeePerDay" -> Set.of("mora", "recargo", "recargo_mora", "latefee");
            case "replacementValue" -> Set.of("reposicion", "valor_reposicion", "repuesto");
            case "stockTotal" -> Set.of("stock", "cantidad", "existencias", "total", "unidades");
            case "minStockThreshold" -> Set.of("minimo", "umbral", "stock_minimo", "alerta");
            case "document" -> Set.of("documento", "cedula", "cc", "nit", "identificacion");
            case "documentType" -> Set.of("tipo_documento", "tipodoc", "tipo");
            case "phone" -> Set.of("telefono", "celular", "whatsapp", "phone");
            case "email" -> Set.of("correo", "email", "mail");
            case "notes" -> Set.of("notas", "observaciones", "nota");
            case "address" -> Set.of("direccion", "address", "obra");
            case "reference" -> Set.of("referencia", "barrio", "sector");
            case "customerName" -> Set.of("cliente", "nombre_cliente", "nombre");
            case "movementType" -> Set.of("tipo", "tipo_movimiento", "naturaleza");
            case "amount" -> Set.of("valor", "monto", "amount", "importe");
            case "movementDate" -> Set.of("fecha", "fecha_movimiento", "date");
            case "originalAmount" -> Set.of("valor", "monto", "deuda", "original");
            case "balance" -> Set.of("saldo", "pendiente", "balance");
            case "dueDate" -> Set.of("vencimiento", "fecha_vencimiento", "vence");
            default -> Set.of(normalize(field));
        };
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String folded = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return folded.replaceAll("[^a-z0-9_/]", "");
    }
}
