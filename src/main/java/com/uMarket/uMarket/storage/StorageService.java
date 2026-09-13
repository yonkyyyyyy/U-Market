package com.uMarket.uMarket.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

    StoredObject store(MultipartFile file) throws IOException;

    InputStream load(String id) throws IOException;

    void delete(String id) throws IOException;

    String getUrl(String id);
}