package com.uMarket.uMarket.event;

/**
 * Evento de dominio para alertas internas del sistema.
 *
 * <p>Desde Spring 4.2 no es necesario extender {@code ApplicationEvent}:
 * cualquier POJO (incluido un record) publicado con
 * {@code ApplicationEventPublisher.publishEvent(...)} es un evento válido.</p>
 *
 * @param usuarioId  destinatario de la alerta (id del {@code Usuario})
 * @param tipoAlerta discriminador (EXPIRACION, MATCH_ENCONTRADO, ...)
 * @param titulo     título corto para mostrar en la bandeja de alertas
 * @param mensaje    detalle de la alerta
 */
public record AlertaInternaEvent(
		Long usuarioId,
		TipoAlerta tipoAlerta,
		String titulo,
		String mensaje
) {
	public AlertaInternaEvent {
		if (usuarioId == null) {
			throw new IllegalArgumentException("usuarioId no puede ser null");
		}
		if (tipoAlerta == null) {
			throw new IllegalArgumentException("tipoAlerta no puede ser null");
		}
		if (titulo == null || titulo.isBlank()) {
			throw new IllegalArgumentException("titulo no puede estar vacío");
		}
	}
}
