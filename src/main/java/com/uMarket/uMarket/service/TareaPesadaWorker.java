package com.uMarket.uMarket.service;

import com.uMarket.uMarket.config.RabbitMQConfig;
import com.uMarket.uMarket.dto.TareaPesada;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Worker asíncrono: consume las tareas pesadas de la cola.
 * La concurrencia real la define {@code spring.rabbitmq.listener.simple.*}
 * en application.yaml.
 */
@Slf4j
@Component
public class TareaPesadaWorker {

	private static final long TIEMPO_TRABAJO_SIMULADO_MS = 1000;

	@RabbitListener(queues = RabbitMQConfig.COLA_TAREAS_PESADAS)
	public void procesar(TareaPesada tarea) {
		log.info("Worker recibió tarea {} (tipo={}, referencia={})",
				tarea.id(), tarea.tipo(), tarea.referencia());

		// Simula el trabajo pesado. En las tareas 2 y 3 esto será el análisis
		// real: NLP sobre texto y visión artificial sobre la imagen.
		try {
			Thread.sleep(TIEMPO_TRABAJO_SIMULADO_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		log.info("Worker terminó tarea {} ({}).", tarea.id(), tarea.tipo());
	}
}