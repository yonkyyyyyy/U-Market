package com.uMarket.uMarket.dto;

import java.util.List;

/**
 * Resultado de una búsqueda en lenguaje natural: la consulta original, cómo se
 * interpretó y los productos ordenados por relevancia.
 */
public record BusquedaNlpResponse(
		String consulta,
		InterpretacionBusqueda interpretacion,
		List<ProductoDto> resultados
) {

	public BusquedaNlpResponse {
		resultados = resultados == null ? List.of() : List.copyOf(resultados);
	}
}
