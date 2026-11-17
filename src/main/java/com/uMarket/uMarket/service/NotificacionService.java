package com.uMarket.uMarket.service;

import com.uMarket.uMarket.model.Producto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envío de notificaciones en tiempo real.
 * <p>
 * Los mensajes privados usan {@code convertAndSendToUser}: el broker los resuelve
 * a {@code /user/{correo}/queue/...}. El cliente se suscribe a {@code /user/queue/notificaciones}.
 * Los mensajes globales van a un destino del broker, p. ej. {@code /topic/catalogo}.
 */
@Service
public class NotificacionService {

	public static final String COLA_NOTIFICACIONES = "/queue/notificaciones";

	private final SimpMessagingTemplate messagingTemplate;

	public NotificacionService(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	/** Envía un payload a la cola privada de un usuario identificado por su correo. */
	public void enviarAUsuario(String correo, String destino, Object payload) {
		messagingTemplate.convertAndSendToUser(correo, destino, payload);
	}

	/** Difunde un evento a todos los suscriptores de un destino del broker. */
	public void difundir(String destino, Object payload) {
		messagingTemplate.convertAndSend(destino, payload);
	}

	/** Ejemplo: aviso al comprador tras una compra exitosa. */
	public void notificarCompra(String correoComprador, Producto producto) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("tipo", "COMPRA_REALIZADA");
		payload.put("mensaje", "Tu compra se registró correctamente");
		payload.put("productoId", producto.getId());
		payload.put("stockRestante", producto.getStock());
		enviarAUsuario(correoComprador, COLA_NOTIFICACIONES, payload);
	}

	/** Ejemplo: aviso al usuario cuya demanda fue bloqueada por otro. */
	public void notificarDemandaBloqueada(String correo, Long demandaId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("tipo", "DEMANDA_BLOQUEADA");
		payload.put("mensaje", "Otro usuario tomó la demanda antes que tú");
		payload.put("demandaId", demandaId);
		enviarAUsuario(correo, COLA_NOTIFICACIONES, payload);
	}
}
