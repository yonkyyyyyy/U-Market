package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record DemandaRequest(

		@NotBlank(message = "El título es obligatorio")
		String titulo,

		String descripcion,

		@DecimalMin(value = "0.01", message = "El presupuesto estimado debe ser mayor a 0")
		BigDecimal presupuestoEstimado
) {
}