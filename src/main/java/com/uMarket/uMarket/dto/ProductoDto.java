package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoDto(Long id, String titulo, String descripcion, BigDecimal precio, String estado,
						  LocalDateTime createdAt, Long usuarioId, String vendedorNombre) {

	public static ProductoDto from(Producto producto) {
		return new ProductoDto(
				producto.getId(),
				producto.getTitulo(),
				producto.getDescripcion(),
				producto.getPrecio(),
				producto.getEstado(),
				producto.getCreatedAt(),
				producto.getUsuario().getId(),
				producto.getUsuario().getNombre());
	}
}