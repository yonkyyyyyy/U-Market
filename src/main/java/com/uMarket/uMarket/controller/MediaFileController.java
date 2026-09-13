package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.storage.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@RestController
@RequestMapping("/uploads")
public class MediaFileController {

	private final StorageService storageService;

	public MediaFileController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/{id}")
	public ResponseEntity<Resource> servir(@PathVariable String id) {
		try {
			InputStream in = storageService.load(id);
			Path ruta = Paths.get(id);
			MediaType contentType = detectarContentType(ruta);
			return ResponseEntity.ok()
					.contentType(contentType)
					.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
					.eTag("\"" + id.hashCode() + "\"")
					.lastModified(System.currentTimeMillis())
					.body(new InputStreamResource(in));
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	private MediaType detectarContentType(Path ruta) {
		try {
			String detectado = Files.probeContentType(ruta);
			if (detectado != null) {
				return MediaType.parseMediaType(detectado);
			}
		} catch (IOException ignored) {
		}
		return MediaType.APPLICATION_OCTET_STREAM;
	}
}