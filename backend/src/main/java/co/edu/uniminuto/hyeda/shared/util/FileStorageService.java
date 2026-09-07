package co.edu.uniminuto.hyeda.shared.util;

import co.edu.uniminuto.hyeda.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${hyeda.storage.path}") String storagePath) throws IOException {
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        Files.createDirectories(this.root.resolve("documents"));
        Files.createDirectories(this.root.resolve("uploads"));
        Files.createDirectories(this.root.resolve("id-copies"));
    }

    public String store(MultipartFile file, String folder) {
        try {
            String name = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path dest = root.resolve(folder).resolve(name);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toString();
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el archivo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Path resolve(String path) {
        return Paths.get(path);
    }

    public Path documentsDir() {
        return root.resolve("documents");
    }

    public Path root() {
        return root;
    }
}
