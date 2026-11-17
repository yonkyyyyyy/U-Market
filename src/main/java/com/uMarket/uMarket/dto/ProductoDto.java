package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoDto(Long id, String titulo, String descripcion, BigDecimal precio, String categoria,
						  String campus, String estado, LocalDateTime createdAt, Long usuarioId,
						  String vendedorNombre) {

	/** Constructor de compatibilidad para los clientes que aún no usan campus. */
	public ProductoDto(Long id, String titulo, String descripcion, BigDecimal precio, String categoria, String estado,
					   LocalDateTime createdAt, Long usuarioId, String vendedorNombre) {
		this(id, titulo, descripcion, precio, categoria, null, estado, createdAt, usuarioId, vendedorNombre);
	}

	public static ProductoDto from(Producto producto) {
		return new ProductoDto(
				producto.getId(),
				producto.getTitulo(),
				producto.getDescripcion(),
				producto.getPrecio(),
				producto.getCategoria(),
				producto.getCampus(),
				producto.getEstado(),
				producto.getCreatedAt(),
				producto.getUsuario().getId(),
				producto.getUsuario().getNombre()
		);
	}
}
