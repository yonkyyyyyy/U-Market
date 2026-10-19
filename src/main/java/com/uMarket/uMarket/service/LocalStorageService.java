package com.uMarket.uMarket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementación de {@link StorageService} en disco local.
 *
 * <p>Funciona porque el worker corre en el MISMO proceso que la API: ambos ven
 * el mismo sistema de archivos. Cada archivo se guarda con un nombre UUID, así
 * que dos subidas simultáneas nunca se pisan.</p>
 */
@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path raiz;

        public LocalStorageService(@Value("${uploads.staging-path:/tmp/uploads/staging}") String ruta) {
        this.raiz = Paths.get(ruta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de staging: " + raiz, e);
        }
        log.info("Staging de imágenes en: {}", raiz);
    }


    @Override
    public String guardar(File archivo) {
        String id = UUID.randomUUID() + extension(archivo.getName());
        try {
            Files.copy(archivo.toPath(), raiz.resolve(id), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen en staging", e);
        }
        return id;
    }

    @Override
    public File leer(String id) {
        return raiz.resolve(id).toFile();
    }

    @Override
    public void borrar(String id) {
        try {
            Files.deleteIfExists(raiz.resolve(id));
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo de staging {}: {}", id, e.getMessage());
        }
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto >= 0 ? nombre.substring(punto) : "";
    }
}
