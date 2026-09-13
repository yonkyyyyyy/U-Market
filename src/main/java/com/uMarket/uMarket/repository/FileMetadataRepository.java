package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.ArchivoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<ArchivoMetadata, Long> {

	List<ArchivoMetadata> findByProductoId(Long productoId);

	Optional<ArchivoMetadata> findByProductoIdAndVariante(Long productoId, String variante);

}