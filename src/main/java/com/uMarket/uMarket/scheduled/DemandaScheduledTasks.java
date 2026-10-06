package com.uMarket.uMarket.scheduled;

import com.uMarket.uMarket.event.AlertaInternaEvent;
import com.uMarket.uMarket.event.TipoAlerta;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.repository.DemandaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class DemandaScheduledTasks {

	private final DemandaRepository demandaRepository;
	private final ApplicationEventPublisher eventPublisher;

	public DemandaScheduledTasks(DemandaRepository demandaRepository,
			ApplicationEventPublisher eventPublisher) {
		this.demandaRepository = demandaRepository;
		this.eventPublisher = eventPublisher;
	}

	// Todos los días a la medianoche
	@Scheduled(cron = "0 0 0 * * ?")
	@Transactional
	public void expirarDemandasAntiguas() {
		LocalDateTime limite = LocalDateTime.now().minusDays(30);
		List<Demanda> expiradas = demandaRepository.findByEstadoAndFechaCreacionBefore("ACTIVA", limite);

		if (expiradas.isEmpty()) {
			log.info("Expiración de demandas: no hay demandas ACTIVA anteriores a {} por cancelar", limite);
			return;
		}

		expiradas.forEach(demanda -> demanda.setEstado("CANCELADA"));
		demandaRepository.saveAll(expiradas);

		log.info("Expiración de demandas: {} demanda(s) ACTIVA con más de 30 días fueron marcadas como CANCELADA", expiradas.size());

		// Publicación desacoplada: el listener persiste la alerta en otro hilo (@Async),
		// por lo que el @Scheduled no se bloquea aunque haya cientos de expiradas.
		expiradas.forEach(demanda -> eventPublisher.publishEvent(new AlertaInternaEvent(
				demanda.getUsuario().getId(),
				TipoAlerta.EXPIRACION,
				"Tu demanda ha expirado",
				"Tu demanda '" + demanda.getTitulo() + "' fue marcada como CANCELADA por antigüedad (>30 días)."
		)));
	}
}
