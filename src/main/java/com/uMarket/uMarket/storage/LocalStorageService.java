package com.uMarket.uMarket.storage;

import com.uMarket.uMarket.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;

    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredObject store(MultipartFile file) throws IOException {
        String id = generateId(file.getOriginalFilename());
        Path root = Paths.get(properties.getLocal().getRoot());
        Files.createDirectories(root);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(id), StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredObject(id, file.getOriginalFilename(), file.getContentType(), file.getSize(), getUrl(id));
    }

    @Override
    public InputStream load(String id) throws IOException {
        return Files.newInputStream(resolve(id));
    }

    @Override
    public void delete(String id) throws IOException {
        Files.deleteIfExists(resolve(id));
    }

    @Override
    public String getUrl(String id) {
        return "/uploads/" + id;
    }

    private Path resolve(String id) throws IOException {
        Path root = Paths.get(properties.getLocal().getRoot()).toAbsolutePath().normalize();
        Path file = root.resolve(id).normalize();
        if (!file.startsWith(root)) {
            throw new IOException("Ruta no valida: " + id);
        }
        return file;
    }

    private String generateId(String originalName) {
        String extension = "";
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                extension = originalName.substring(dot);
            }
        }
        return UUID.randomUUID() + extension;
    }
}