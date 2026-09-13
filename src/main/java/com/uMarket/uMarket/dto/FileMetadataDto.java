package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.model.ArchivoMetadata;

import java.time.LocalDateTime;

public record FileMetadataDto(Long id, Long productoId, String variante, String nombreOriginal,
							  String contentType, Long sizeBytes, String url, LocalDateTime fechaSubida) {

	public static FileMetadataDto from(ArchivoMetadata metadata) {
		return new FileMetadataDto(
				metadata.getId(),
				metadata.getProductoId(),
				metadata.getVariante(),
				metadata.getNombreOriginal(),
				metadata.getContentType(),
				metadata.getSizeBytes(),
				metadata.getUrl(),
				metadata.getFechaSubida());
	}
}