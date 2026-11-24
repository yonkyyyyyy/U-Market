package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record GenerarCopyRequest(

		@NotBlank(message = "El título del producto es obligatorio")
		@Size(max = 150, message = "El título no debe superar los 150 caracteres")
		String titulo,

		@Size(max = 2000, message = "La descripción no debe superar los 2000 caracteres")
		String descripcion,

		@DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
		BigDecimal precio,

		@Size(max = 80, message = "El campus no debe superar los 80 caracteres")
		String campus,

		@NotBlank(message = "La plataforma es obligatoria")
		@Pattern(regexp = "(?i)(Instagram|Facebook|WhatsApp|Marketplace)",
				message = "La plataforma seleccionada no es válida")
		String plataforma,

		@NotBlank(message = "El tono es obligatorio")
		@Pattern(regexp = "(?i)(Cercano|Profesional|Divertido|Persuasivo)",
				message = "El tono seleccionado no es válido")
		String tono,

		@Size(max = 120, message = "El público objetivo no debe superar los 120 caracteres")
		String publicoObjetivo
) {
}
