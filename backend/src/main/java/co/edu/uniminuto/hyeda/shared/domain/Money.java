package co.edu.uniminuto.hyeda.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aritmetica monetaria en pesos colombianos.
 *
 * <p>Todo el dinero del sistema pasa por aqui para que el contrato, la factura
 * y las analiticas siempre lleguen al mismo numero. Se redondea a 2 decimales
 * con HALF_UP, que es el criterio comercial habitual.
 */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int SCALE = 2;

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? ZERO : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                total = total.add(value);
            }
        }
        return of(total);
    }

    public static BigDecimal subtract(BigDecimal minuend, BigDecimal subtrahend) {
        return of(nullSafe(minuend).subtract(nullSafe(subtrahend)));
    }

    public static BigDecimal multiply(BigDecimal value, long factor) {
        return of(nullSafe(value).multiply(BigDecimal.valueOf(factor)));
    }

    /** Nunca deja que un cobro quede en negativo. */
    public static BigDecimal atLeastZero(BigDecimal value) {
        BigDecimal safe = of(value);
        return safe.signum() < 0 ? ZERO : safe;
    }

    public static boolean isPositive(BigDecimal value) {
        return nullSafe(value).signum() > 0;
    }

    public static boolean isZero(BigDecimal value) {
        return nullSafe(value).signum() == 0;
    }

    public static boolean gte(BigDecimal left, BigDecimal right) {
        return nullSafe(left).compareTo(nullSafe(right)) >= 0;
    }

    public static boolean gt(BigDecimal left, BigDecimal right) {
        return nullSafe(left).compareTo(nullSafe(right)) > 0;
    }

    /** Formato para documentos PDF y mensajes de WhatsApp: {@code $ 1.250.000}. */
    public static String format(BigDecimal value) {
        BigDecimal safe = of(value);
        String digits = safe.setScale(0, RoundingMode.HALF_UP).abs().toPlainString();
        StringBuilder grouped = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            grouped.append(digits.charAt(i));
            if (++count % 3 == 0 && i > 0) {
                grouped.append('.');
            }
        }
        String body = grouped.reverse().toString();
        return (safe.signum() < 0 ? "-$ " : "$ ") + body;
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
