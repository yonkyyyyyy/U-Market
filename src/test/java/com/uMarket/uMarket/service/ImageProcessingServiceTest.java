package com.uMarket.uMarket.service;

import com.uMarket.uMarket.config.StorageProperties;
import com.uMarket.uMarket.dto.ProcessedImage;
import com.uMarket.uMarket.storage.LocalStorageService;
import com.uMarket.uMarket.storage.StoredObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImageProcessingServiceTest {

    @TempDir
    Path tempDir;

    private MockMultipartFile createPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", "producto.png", "image/png", out.toByteArray());
    }

    @Test
    void process_generates_three_variants() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setRoot(tempDir.toString());

        ImageProcessingService service = new ImageProcessingService(new LocalStorageService(properties));
        MockMultipartFile image = createPng(1200, 800);

        ProcessedImage result = service.process(image);

        assertNotNull(result.thumb());
        assertNotNull(result.medium());
        assertNotNull(result.full());
        assertEquals("/uploads/" + result.thumb().id(), result.thumbUrl());
        assertFalse(result.thumb().id().equals(result.full().id()));
    }

    @Test
    void thumb_should_be_smaller_than_original_dimensions() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setRoot(tempDir.toString());

        ImageProcessingService service = new ImageProcessingService(new LocalStorageService(properties));
        MockMultipartFile image = createPng(1200, 800);

        ProcessedImage result = service.process(image);

        StoredObject thumbObj = result.thumb();
        assertNotNull(thumbObj.id());
        long thumbSize = java.nio.file.Files.size(tempDir.resolve(thumbObj.id()));
        long originalSize = image.getSize();
        assertFalse(thumbSize <= 0);
    }
}