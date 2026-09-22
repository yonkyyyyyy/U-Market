package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoRequest(

		@NotBlank(message = "El título es obligatorio")
		String titulo,

		String descripcion,

		@NotNull(message = "El precio es obligatorio")
		@DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
		BigDecimal precio,

		String estado
) {
}