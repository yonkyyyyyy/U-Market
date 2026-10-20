package com.uMarket.uMarket.dto;

import java.util.List;
import java.util.Map;

/**
 * Respuesta de la API de moderación de OpenAI (POST /v1/moderations).
 *
 * <p>Los nombres de campo coinciden exactamente con el JSON devuelto por
 * OpenAI (Jackson mapea 1:1, sin anotaciones necesarias).</p>
 */
public record OpenAIModeracionResponse(
		String id,
		String model,
		List<OpenAIModeracionResult> results
) {

	public record OpenAIModeracionResult(
			boolean flagged,
			Map<String, Boolean> categories,
			Map<String, Double> category_scores
	) {

		/**
		 * Score máximo entre todas las categorías detectadas (0..1).
		 * Es el nivel de confianza de que el contenido es inapropiado.
		 */
		public double maxScore() {
			return category_scores.values().stream()
					.mapToDouble(Double::doubleValue)
					.max()
					.orElse(0.0);
		}

		/**
		 * Categorías cuyo score superó el umbral o que fueron marcadas.
		 */
		public List<String> categoriasDetectadas() {
			return categories.entrySet().stream()
					.filter(Map.Entry::getValue)
					.map(Map.Entry::getKey)
					.sorted()
					.toList();
		}
	}
}