package co.edu.uniminuto.hyeda.shared.storage;

import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

/**
 * Guarda archivos fuera de la base: PDFs, firmas, cedulas y evidencias.
 * Las rutas persistidas son relativas a {@code hyeda.storage-path}.
 */
@Slf4j
@Service
public class FileStorageService {

    public static final String DOCUMENTS = "documentos";
    public static final String REPORTS = "reportes";
    public static final String IMPORTS = "importaciones";
    public static final String SIGNATURES = "firmas";
    public static final String EVIDENCE = "evidencias";
    public static final String ID_COPIES = "cedulas";

    private final HyedaProperties properties;
    private Path root;

    public FileStorageService(HyedaProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        root = Path.of(properties.getStoragePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento: " + root, e);
        }
        log.info("Almacenamiento de archivos en {}", root);
    }

    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo esta vacio");
        }
        String original = file.getOriginalFilename() == null ? "archivo" : file.getOriginalFilename();
        try {
            return storeBytes(file.getBytes(), folder, extensionOf(original));
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el archivo");
        }
    }

    public String storeBytes(byte[] content, String folder, String extension) {
        if (content == null || content.length == 0) {
            throw new BusinessException("El archivo esta vacio");
        }
        Path dir = safeResolve(safeFolder(folder));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "." + sanitizeExtension(extension));
            Files.write(target, content);
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el archivo");
        }
    }

    public byte[] read(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("El archivo no existe");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo");
        }
    }

    public boolean exists(String relativePath) {
        return relativePath != null && !relativePath.isBlank() && Files.exists(resolve(relativePath));
    }

    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResourceNotFoundException("Ruta de archivo vacia");
        }
        return safeResolve(relativePath);
    }

    private Path safeResolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException("Ruta de archivo invalida");
        }
        return path;
    }

    private String safeFolder(String folder) {
        if (folder == null || folder.isBlank() || folder.contains("..")
                || folder.contains("/") || folder.contains("\\")) {
            throw new BusinessException("Carpeta de almacenamiento invalida");
        }
        return folder;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "bin";
        }
        return sanitizeExtension(filename.substring(dot + 1));
    }

    private String sanitizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "bin";
        }
        return extension.toLowerCase(Locale.ROOT).replace(".", "");
    }
}
