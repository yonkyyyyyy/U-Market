package com.uMarket.uMarket.dto;

import java.util.List;

/**
 * Respuesta de la API de chat de OpenAI (POST /v1/chat/completions) cuando se
 * le envía una imagen para análisis de visión.
 *
 * <p>Los nombres de campo coinciden con el JSON devuelto por OpenAI
 * (Jackson mapea 1:1, sin anotaciones necesarias).</p>
 */
public record VisionAnalisisResponse(
		String id,
		String model,
		List<Choice> choices
) {

	public record Choice(
			int index,
			Message message,
			String finish_reason
	) {
	}

	public record Message(
			String role,
			String content
	) {
	}

	/**
	 * Texto (JSON) del primer "choice" de la respuesta, o {@code null} si
	 * la respuesta vino vacía.
	 */
	public String contenido() {
		if (choices == null || choices.isEmpty()) {
			return null;
		}
		Message message = choices.get(0).message();
		return message == null ? null : message.content();
	}
}
