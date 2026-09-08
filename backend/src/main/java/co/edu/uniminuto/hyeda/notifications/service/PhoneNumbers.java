package co.edu.uniminuto.hyeda.notifications.service;

/**
 * Normalizacion de telefonos a formato internacional.
 *
 * <p>En los archivos de la empresa los numeros vienen escritos de mil maneras
 * ("320 408 9057", "0000000000", "+57 320..."); WhatsApp solo acepta digitos con
 * indicativo de pais.
 */
public final class PhoneNumbers {

    private static final String COLOMBIA = "57";

    private PhoneNumbers() {
    }

    /** @return numero listo para WhatsApp, o {@code null} si no es utilizable */
    public static String toWhatsApp(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        // Un celular colombiano tiene 10 digitos y empieza por 3.
        if (digits.length() == 10 && digits.startsWith("3")) {
            return COLOMBIA + digits;
        }
        if (digits.length() == 12 && digits.startsWith(COLOMBIA)) {
            return digits;
        }
        // Fijo local de siete digitos: no sirve para WhatsApp.
        return digits.length() >= 11 ? digits : null;
    }

    public static boolean isUsable(String raw) {
        return toWhatsApp(raw) != null;
    }
}
