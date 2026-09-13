package com.uMarket.uMarket.storage;

import com.uMarket.uMarket.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void store_load_and_delete_roundtrip() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setRoot(tempDir.toString());

        LocalStorageService service = new LocalStorageService(properties);
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.png", "image/png", "contenido-de-prueba".getBytes(StandardCharsets.UTF_8));

        StoredObject stored = service.store(file);

        assertTrue(stored.id().endsWith(".png"));
        assertEquals("image/png", stored.contentType());
        assertEquals(file.getSize(), stored.size());
        assertEquals("/uploads/" + stored.id(), stored.url());

        try (InputStream in = service.load(stored.id())) {
            assertEquals("contenido-de-prueba", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }

        service.delete(stored.id());
        assertTrue(!java.nio.file.Files.exists(tempDir.resolve(stored.id())));
    }
}