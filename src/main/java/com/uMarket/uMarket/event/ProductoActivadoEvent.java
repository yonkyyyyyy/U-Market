package com.uMarket.uMarket.event;

/**
 * Evento que se publica cuando una publicación pasa a {@code ACTIVO}.
 *
 * <p>Dispara la <b>demanda inversa</b>: buscar demandas activas compatibles
 * (campus, etiquetas, categoría, texto y precio) y avisar a sus autores.</p>
 *
 * @param productoId publicación que acaba de activarse
 */
public record ProductoActivadoEvent(Long productoId) {

	public ProductoActivadoEvent {
		if (productoId == null) {
			throw new IllegalArgumentException("productoId no puede ser null");
		}
	}
}
