package com.uMarket.uMarket.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON que se le pide al modelo de chat para interpretar una búsqueda.
 *
 * <p>Las claves coinciden 1:1 con las del objeto JSON solicitado en el prompt
 * (camelCase), por lo que Jackson las mapea sin anotaciones.</p>
 */
public record InterpretacionLlm(
		List<String> palabrasClave,
		String categoria,
		String campus,
		BigDecimal precioMaximo,
		List<String> tags
) {
}
