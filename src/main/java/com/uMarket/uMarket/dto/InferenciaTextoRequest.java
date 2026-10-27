package com.uMarket.uMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Petición de inferencia de TEXTO de la API interna de IA
 * ({@code POST /api/ia/v1/texto}).
 *
 * <p>Solo lleva el texto a moderar; el llamador no necesita conocer el modelo ni
 * el umbral, que los resuelve el servidor.</p>
 */
@Schema(description = "Texto a moderar (por ejemplo, el título + la descripción de una publicación)")
public record InferenciaTextoRequest(

		@Schema(description = "Texto a analizar", example = "Vendo laptop en buen estado, poco uso")
		@NotBlank(message = "texto no puede estar vacío")
		String texto
) {
}
