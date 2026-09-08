package co.edu.uniminuto.hyeda.iam.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Genera usuario y contrasena para empleados nuevos.
 *
 * <p>La contrasena se arma con caracteres sin ambiguedad visual (sin l, I, 1,
 * O, 0) porque en la practica se dicta o se copia a mano en el mostrador.
 */
@Component
public class CredentialGenerator {

    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%&*";
    private static final String ALL = LOWER + UPPER + DIGITS + SYMBOLS;
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generatePassword() {
        StringBuilder password = new StringBuilder(LENGTH);
        // Se garantiza al menos un caracter de cada familia.
        password.append(pick(LOWER)).append(pick(UPPER)).append(pick(DIGITS)).append(pick(SYMBOLS));
        while (password.length() < LENGTH) {
            password.append(pick(ALL));
        }
        return shuffle(password.toString());
    }

    /**
     * Deriva un usuario a partir del nombre completo: "la propietaria Mora" -> "ymora".
     * Si ya existe, agrega un sufijo numerico.
     */
    public String generateUsername(String fullName, Predicate<String> exists) {
        String normalized = Normalizer.normalize(fullName == null ? "" : fullName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z ]", "")
                .trim();

        String[] parts = normalized.split("\\s+");
        String base;
        if (parts.length >= 2 && !parts[0].isBlank()) {
            base = parts[0].charAt(0) + parts[1];
        } else if (!normalized.isBlank()) {
            base = parts[0];
        } else {
            base = "usuario";
        }
        base = base.length() > 18 ? base.substring(0, 18) : base;

        String candidate = base;
        int suffix = 1;
        while (exists.test(candidate)) {
            candidate = base + (++suffix);
        }
        return candidate;
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    private String shuffle(String value) {
        char[] chars = value.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
