package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(

		@NotBlank(message = "El título es obligatorio")
		String titulo,

		String descripcion,

		@NotNull(message = "El precio es obligatorio")
		@DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
		BigDecimal precio,

		@Size(max = 80, message = "La categoría no debe superar los 80 caracteres")
		String categoria,

		String estado,

		@Size(max = 80, message = "El campus no debe superar los 80 caracteres")
		String campus
) {

	/** Constructor de compatibilidad para los clientes que aún no envían campus. */
	public ProductoRequest(String titulo, String descripcion, BigDecimal precio, String categoria, String estado) {
		this(titulo, descripcion, precio, categoria, estado, null);
	}
}
