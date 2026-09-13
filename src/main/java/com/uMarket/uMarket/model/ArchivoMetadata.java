package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "archivos_metadata")
@Getter
@Setter
@NoArgsConstructor
public class ArchivoMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "producto_id", nullable = false)
	private Long productoId;

	@Column(name = "variante", nullable = false, length = 20)
	private String variante;

	@Column(name = "nombre_original", length = 255)
	private String nombreOriginal;

	@Column(name = "content_type", length = 100)
	private String contentType;

	@Column(name = "size_bytes")
	private Long sizeBytes;

	@Column(name = "url", length = 500)
	private String url;

	@Column(name = "fecha_subida")
	private LocalDateTime fechaSubida;

}