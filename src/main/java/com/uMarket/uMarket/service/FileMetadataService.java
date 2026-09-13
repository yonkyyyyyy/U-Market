package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.FileMetadataDto;
import com.uMarket.uMarket.dto.ProcessedImage;
import com.uMarket.uMarket.model.ArchivoMetadata;
import com.uMarket.uMarket.repository.FileMetadataRepository;
import com.uMarket.uMarket.storage.StoredObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileMetadataService {

	private final FileMetadataRepository fileMetadataRepository;

	public FileMetadataService(FileMetadataRepository fileMetadataRepository) {
		this.fileMetadataRepository = fileMetadataRepository;
	}

	@Transactional
	public void registrarVariantes(Long productoId, ProcessedImage procesada) {
		registrar(productoId, "THUMB", procesada.thumb());
		registrar(productoId, "MEDIUM", procesada.medium());
		registrar(productoId, "FULL", procesada.full());
	}

	@Transactional(readOnly = true)
	public List<FileMetadataDto> listarPorProducto(Long productoId) {
		return fileMetadataRepository.findByProductoId(productoId)
				.stream().map(FileMetadataDto::from).toList();
	}

	private void registrar(Long productoId, String variante, StoredObject objeto) {
		ArchivoMetadata metadata = new ArchivoMetadata();
		metadata.setProductoId(productoId);
		metadata.setVariante(variante);
		metadata.setNombreOriginal(objeto.originalName());
		metadata.setContentType(objeto.contentType());
		metadata.setSizeBytes(objeto.size());
		metadata.setUrl(objeto.url());
		metadata.setFechaSubida(LocalDateTime.now());
		fileMetadataRepository.save(metadata);
	}
}