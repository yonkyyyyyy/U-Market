package com.uMarket.uMarket.service;

import com.uMarket.uMarket.config.ImageVariant;
import com.uMarket.uMarket.dto.ProcessedImage;
import com.uMarket.uMarket.storage.InMemoryMultipartFile;
import com.uMarket.uMarket.storage.StorageService;
import com.uMarket.uMarket.storage.StoredObject;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);
    private static final float QUALITY = 0.8f;

    private final StorageService storageService;

    public ImageProcessingService(StorageService storageService) {
        this.storageService = storageService;
    }

    public ProcessedImage process(MultipartFile file) throws IOException {
        byte[] original = file.getBytes();
        ImageVariant[] variants = ImageVariant.values();

        StoredObject thumb = storeVariant(file, variantName(file, ImageVariant.THUMB), resize(original, variants[0]));
        StoredObject medium = storeVariant(file, variantName(file, ImageVariant.MEDIUM), resize(original, variants[1]));
        StoredObject full = storeVariant(file, variantName(file, ImageVariant.FULL), resize(original, variants[2]));

        log.info("Imagen procesada: thumb={}, medium={}, full={}", thumb.id(), medium.id(), full.id());
        return new ProcessedImage(thumb, medium, full);
    }

    private byte[] resize(byte[] source, ImageVariant variant) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(source);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Thumbnails.Builder<?> builder = Thumbnails.of(in);
            if (variant.isResize()) {
                builder.size(variant.getMaxWidth(), variant.getMaxHeight());
            } else {
                builder.scale(1.0);
            }
            builder.outputQuality(QUALITY);
            builder.toOutputStream(out);
            return out.toByteArray();
        }
    }

    private StoredObject storeVariant(MultipartFile original, String name, byte[] data) throws IOException {
        return storageService.store(new InMemoryMultipartFile(data, name, original.getContentType(), name));
    }

    private String variantName(MultipartFile original, ImageVariant variant) {
        return variant.name().toLowerCase() + generateSuffix(original.getOriginalFilename());
    }

    private String generateSuffix(String originalName) {
        if (originalName == null) {
            return ".jpg";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            return originalName.substring(dot);
        }
        return ".jpg";
    }
}