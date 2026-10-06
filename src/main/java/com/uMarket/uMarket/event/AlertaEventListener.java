package com.uMarket.uMarket.event;

import com.uMarket.uMarket.model.Alerta;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.AlertaRepository;
import com.uMarket.uMarket.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suscriptor de {@link AlertaInternaEvent}.
 *
 * <p>Cada método corre en el pool {@code "alertaExecutor"} gracias a
 * {@code @Async}, por lo que el publicador (el @Scheduled o el request
 * HTTP) no se bloquea mientras se persiste la alerta.</p>
 *
 * <p>Nota: si el evento se publica dentro de una transacción y necesitas
 * garantizar que el listener solo corra tras un commit exitoso, cambia
 * {@code @EventListener} por
 * {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)}.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlertaEventListener {

	private final AlertaRepository alertaRepository;
	private final UsuarioRepository usuarioRepository;

    public AlertaEventListener(AlertaRepository alertaRepository, UsuarioRepository usuarioRepository) {
        this.alertaRepository = alertaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Async("alertaExecutor")
	@EventListener(condition = "#event.tipoAlerta.name() == 'EXPIRACION'")
	@Transactional
	public void onExpiracion(AlertaInternaEvent event) {
		log.info("[EXPIRACION] Alerta para usuarioId={} | {} | hilo={}",
				event.usuarioId(), event.titulo(), Thread.currentThread().getName());
		persistir(event);
	}

	@Async("alertaExecutor")
	@EventListener(condition = "#event.tipoAlerta.name() == 'MATCH_ENCONTRADO'")
	@Transactional
	public void onMatchEncontrado(AlertaInternaEvent event) {
		log.info("[MATCH_ENCONTRADO] Alerta para usuarioId={} | {} | hilo={}",
				event.usuarioId(), event.titulo(), Thread.currentThread().getName());
		persistir(event);
	}

	private void persistir(AlertaInternaEvent event) {
		Usuario destinatario = usuarioRepository.findById(event.usuarioId())
				.orElse(null);
		if (destinatario == null) {
			log.warn("No se pudo persistir la alerta: usuario no encontrado (id={})", event.usuarioId());
			return;
		}
		Alerta alerta = new Alerta();
		alerta.setUsuario(destinatario);
		alerta.setTipoAlerta(event.tipoAlerta());
		alerta.setTitulo(event.titulo());
		alerta.setMensaje(event.mensaje());
		alerta.setLeida(false);
		alertaRepository.save(alerta);
		log.info("Alerta persistida: id destinatario={} tipo={}", event.usuarioId(), event.tipoAlerta());
	}
}
