package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BloqueoDemandaRequest(

		@NotBlank(message = "El motivo del bloqueo es obligatorio")
		@Size(max = 2000, message = "El motivo no debe superar los 2000 caracteres")
		String motivo
) {
}
