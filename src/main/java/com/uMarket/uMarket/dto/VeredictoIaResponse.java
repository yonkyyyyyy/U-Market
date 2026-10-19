package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Veredicto que devuelve la API interna de inferencia de IA
 * ({@code POST /api/ia/v1/texto} y {@code POST /api/ia/v1/imagen}).
 *
 * <p>Separa dos cosas que conviene no mezclar:</p>
 * <ul>
 *   <li>{@code veredicto}: lo que dijo la IA, <b>crudo y trazable</b>
 *       ({@code APROBADO}, {@code RECHAZADO} o {@code FALLIDO}).</li>
 *   <li>{@code permitido}: la <b>decisión ya aplicada</b> con la política del tipo
 *       ({@code TEXTO} es fail-open y {@code IMAGEN} fail-closed), para que el
 *       llamador no tenga que interpretar nada.</li>
 * </ul>
 *
 * <p>Así, si la IA no estuvo disponible ({@code veredicto = FALLIDO}), el
 * llamador ve el fallo pero igual recibe una decisión coherente con el resto del
 * pipeline: texto permitido, imagen bloqueada.</p>
 */
@Schema(description = "Veredicto de inferencia: resultado crudo de la IA + decisión ya aplicada")
public record VeredictoIaResponse(

		@Schema(description = "Resultado crudo de la IA", example = "APROBADO")
		VeredictoModeracion veredicto,

		@Schema(description = "Decisión aplicada según la política del tipo (texto fail-open, imagen fail-closed)",
				example = "true")
		boolean permitido,

		@Schema(description = "Nivel de confianza de la IA en la categoría más fuerte (0..1)", example = "0.02")
		double score,

		@Schema(description = "Categorías detectadas (vacío si no se detectó ninguna)")
		List<String> categorias,

		@Schema(description = "Modelo de OpenAI que emitió el veredicto", example = "omni-moderation-latest")
		String modelo,

		@Schema(description = "Qué se analizó", example = "TEXTO")
		TipoModeracion tipo,

		@Schema(description = "Cuánto tardó la inferencia (ms)", example = "812")
		long latenciaMs,

		@Schema(description = "Momento en que se evaluó")
		LocalDateTime evaluadoEn
) {

	public VeredictoIaResponse {
		if (categorias == null) {
			categorias = List.of();
		}
	}
}
