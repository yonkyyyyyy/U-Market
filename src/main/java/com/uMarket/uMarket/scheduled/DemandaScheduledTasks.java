package com.uMarket.uMarket.scheduled;

import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.repository.DemandaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class DemandaScheduledTasks {

	private final DemandaRepository demandaRepository;

	public DemandaScheduledTasks(DemandaRepository demandaRepository) {
		this.demandaRepository = demandaRepository;
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
	}
}
