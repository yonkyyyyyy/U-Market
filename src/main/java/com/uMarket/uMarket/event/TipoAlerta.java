package com.uMarket.uMarket.event;

/**
 * Tipos de alerta interna del sistema.
 * Usar enum en lugar de String evita errores de tipeo
 * ('EXPIRACION' vs 'EXPIRACIÓN') y permite filtrar listeners con SpEL.
 */
public enum TipoAlerta {
	EXPIRACION,
	MATCH_ENCONTRADO
}
