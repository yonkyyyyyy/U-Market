package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DemandaRequestDTO(

		@NotBlank(message = "El título es obligatorio")
		@Size(max = 150, message = "El título no debe superar los 150 caracteres")
		String titulo,

		@Size(max = 2000, message = "La descripción es muy larga")
		String descripcion,

		@Positive(message = "El presupuesto debe ser mayor a 0")
		BigDecimal presupuestoEstimado,

		@Size(max = 80, message = "La categoría no debe superar los 80 caracteres")
		String categoria
) {
}
