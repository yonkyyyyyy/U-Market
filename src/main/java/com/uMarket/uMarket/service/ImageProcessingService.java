package com.uMarket.uMarket.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
public class ImageProcessingService {

    public File compressAndResizeImage(MultipartFile file) throws IOException {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);

        File tempFile = File.createTempFile("img_" + UUID.randomUUID(), "." + extension);

        Thumbnails.of(file.getInputStream())
                .size(800, 800)
                .outputQuality(0.8)
                .toFile(tempFile);

        return tempFile;
    }
}
