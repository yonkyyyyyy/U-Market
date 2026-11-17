package com.uMarket.uMarket.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interpretación estructurada de una consulta escrita en lenguaje natural.
 *
 * <p>La produce el LLM ({@code origen = "LLM"}) o, si la API no está
 * disponible, el parser heurístico local ({@code origen = "HEURISTICA"}).
 * El frontend la muestra para que el usuario entienda qué se buscó.</p>
 *
 * @param palabrasClave términos relevantes extraídos de la consulta
 * @param categoria     categoría del catálogo detectada (o {@code null})
 * @param campus        campus detectado (o {@code null})
 * @param precioMaximo  presupuesto máximo en soles (o {@code null})
 * @param tags          etiquetas adicionales detectadas (o lista vacía)
 * @param origen        {@code "LLM"} o {@code "HEURISTICA"}
 */
public record InterpretacionBusqueda(
		List<String> palabrasClave,
		String categoria,
		String campus,
		BigDecimal precioMaximo,
		List<String> tags,
		String origen
) {

	public InterpretacionBusqueda {
		palabrasClave = palabrasClave == null ? List.of() : List.copyOf(palabrasClave);
		tags = tags == null ? List.of() : List.copyOf(tags);
	}

	/** Indica si la consulta no aportó ningún criterio utilizable. */
	public boolean vacia() {
		return palabrasClave.isEmpty()
				&& (categoria == null || categoria.isBlank())
				&& (campus == null || campus.isBlank())
				&& precioMaximo == null
				&& tags.isEmpty();
	}
}
