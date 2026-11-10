package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.NotificacionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ejemplo mínimo de disparo de notificaciones en tiempo real. El servicio real
 * (compra, bloqueo de demanda, etc.) llama a {@link NotificacionService}.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

	private final NotificacionService notificacionService;

	public NotificacionController(NotificacionService notificacionService) {
		this.notificacionService = notificacionService;
	}

	/**
	 * Envía una notificación de prueba al usuario autenticado. El cliente debe estar
	 * suscrito a {@code /user/queue/notificaciones}.
	 */
	@PostMapping("/demo")
	public Map<String, Object> demo(@AuthenticationPrincipal UsuarioPrincipal principal) {
		String correo = principal.getUsuario().getCorreoInstitucional();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("tipo", "DEMO");
		payload.put("mensaje", "Notificación de prueba desde U-Market");
		notificacionService.enviarAUsuario(correo, NotificacionService.COLA_NOTIFICACIONES, payload);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("mensaje", "Notificación enviada");
		response.put("destino", "/user/" + correo + NotificacionService.COLA_NOTIFICACIONES);
		return response;
	}
}
