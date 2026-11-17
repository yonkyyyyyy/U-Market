package com.uMarket.uMarket.dto;

import java.util.List;

/**
 * Respuesta de {@code POST /v1/chat/completions} usada por la búsqueda NLP.
 *
 * <p>Estructuralmente es la misma que la de visión, pero se mantiene un DTO
 * propio para no acoplar dos funcionalidades distintas al mismo contrato.</p>
 */
public record BusquedaChatResponse(
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

	/** Texto (JSON) del primer "choice", o {@code null} si vino vacío. */
	public String contenido() {
		if (choices == null || choices.isEmpty()) {
			return null;
		}
		Message message = choices.get(0).message();
		return message == null ? null : message.content();
	}
}
