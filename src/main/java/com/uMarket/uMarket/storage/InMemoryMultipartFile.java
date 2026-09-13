package com.uMarket.uMarket.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InMemoryMultipartFile implements MultipartFile {

    private final byte[] data;
    private final String name;
    private final String contentType;
    private final String originalFilename;

    public InMemoryMultipartFile(byte[] data, String name, String contentType, String originalFilename) {
        this.data = data;
        this.name = name;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return data == null || data.length == 0;
    }

    @Override
    public long getSize() {
        return data != null ? data.length : 0;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        java.nio.file.Files.write(dest.toPath(), data);
    }
}