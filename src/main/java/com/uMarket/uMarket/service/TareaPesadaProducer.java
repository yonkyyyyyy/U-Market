package com.uMarket.uMarket.service;

import com.uMarket.uMarket.config.RabbitMQConfig;
import com.uMarket.uMarket.dto.TareaPesada;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Productor: publica tareas pesadas en la cola {@code tareas.pesadas}.
 * El worker las consume de forma asíncrona.
 */
@Service
public class TareaPesadaProducer {

	private final RabbitTemplate rabbitTemplate;

	public TareaPesadaProducer(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publicar(TareaPesada tarea) {
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_TAREAS,
				RabbitMQConfig.ROUTING_TAREA_PESADA,
				tarea);
	}
}