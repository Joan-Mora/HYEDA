package co.edu.uniminuto.hyeda.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversion de numeros a letras en espanol.
 *
 * <p>Los formatos de remision y devolucion que usa la empresa piden la cantidad
 * en numero y en letras, porque asi es mas dificil alterar un documento firmado.
 */
public final class SpanishNumbers {

    private static final String[] UNITS = {
            "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
            "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete",
            "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidos", "veintitres",
            "veinticuatro", "veinticinco", "veintiseis", "veintisiete", "veintiocho", "veintinueve"
    };

    private static final String[] TENS = {
            "", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };

    private static final String[] HUNDREDS = {
            "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
            "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    private SpanishNumbers() {
    }

    /** {@code 21 -> "Veintiuno"}. Con la primera letra en mayuscula, para tablas. */
    public static String capitalized(long value) {
        String words = words(value);
        return words.substring(0, 1).toUpperCase() + words.substring(1);
    }

    public static String words(long value) {
        if (value < 0) {
            return "menos " + words(-value);
        }
        if (value < 30) {
            return UNITS[(int) value];
        }
        if (value < 100) {
            long tens = value / 10;
            long rest = value % 10;
            return TENS[(int) tens] + (rest == 0 ? "" : " y " + UNITS[(int) rest]);
        }
        if (value == 100) {
            return "cien";
        }
        if (value < 1_000) {
            long hundreds = value / 100;
            long rest = value % 100;
            return HUNDREDS[(int) hundreds] + (rest == 0 ? "" : " " + words(rest));
        }
        if (value < 1_000_000) {
            long thousands = value / 1_000;
            long rest = value % 1_000;
            String prefix = thousands == 1 ? "mil" : words(thousands) + " mil";
            return prefix + (rest == 0 ? "" : " " + words(rest));
        }
        long millions = value / 1_000_000;
        long rest = value % 1_000_000;
        String prefix = millions == 1 ? "un millon" : words(millions) + " millones";
        return prefix + (rest == 0 ? "" : " " + words(rest));
    }

    /** Importe en letras para documentos: {@code "Doscientos veinte mil pesos m/cte"}. */
    public static String pesos(BigDecimal amount) {
        long value = Money.of(amount).setScale(0, RoundingMode.HALF_UP).longValue();
        return capitalized(value) + (value == 1 ? " peso m/cte" : " pesos m/cte");
    }
}
