package com.uMarket.uMarket.dto;

/**
 * Evento de dominio y payload STOMP del cambio de stock.
 * Se publica como Spring Event dentro de la transacción de compra y solo se
 * difunde a los clientes cuando la transacción hace commit.
 */
public record StockUpdateEventDTO(Long productoId, int nuevoStock) {
}
