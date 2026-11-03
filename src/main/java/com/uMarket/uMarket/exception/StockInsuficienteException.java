package com.uMarket.uMarket.exception;

/**
 * Se lanza cuando el UPDATE condicional de stock no afecta filas: otra compra
 * agotó las unidades antes. RuntimeException para que {@code @Transactional} haga rollback.
 * El {@code GlobalExceptionHandler} la traduce a HTTP 409 Conflict.
 */
public class StockInsuficienteException extends RuntimeException {

	public StockInsuficienteException(Long productoId) {
		super("No hay stock suficiente para procesar su compra. El producto se agotó hace unos instantes.");
	}
}
