package com.uMarket.uMarket.dto;

import java.util.List;

/**
 * Veredicto que el modelo de visión devuelve como JSON dentro del mensaje de
 * la respuesta de chat.
 *
 * <p>Formato esperado:</p>
 * <pre>{"prohibido": true, "confianza": 0.95, "categorias": ["droga"]}</pre>
 */
public record VisionVeredicto(
		boolean prohibido,
		double confianza,
		List<String> categorias
) {

	public VisionVeredicto {
		if (categorias == null) {
			categorias = List.of();
		}
	}
}
