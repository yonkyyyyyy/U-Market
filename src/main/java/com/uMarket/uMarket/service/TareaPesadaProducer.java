package com.uMarket.uMarket.service;

import com.uMarket.uMarket.config.RabbitMQConfig;
import com.uMarket.uMarket.dto.TareaPesada;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Productor: publica tareas pesadas en la cola {@code productos.moderacion}.
 * El worker las consume de forma asíncrona.
 */
@Service
public class TareaPesadaProducer {

	private final RabbitTemplate rabbitTemplate;

	public TareaPesadaProducer(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	/**
	 * Publica la tarea. Si hay una transacción de BD activa, espera al COMMIT
	 * para enviarla: así el worker nunca recibe un mensaje de una fila que
	 * todavía no existe (evita la carrera "publicar antes de commitear", que
	 * dejaría la imagen en revisión sin procesar).
	 */
	public void publicar(TareaPesada tarea) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					enviar(tarea);
				}
			});
		} else {
			enviar(tarea);
		}
	}

	private void enviar(TareaPesada tarea) {
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_TAREAS,
				RabbitMQConfig.ROUTING_MODERACION,
				tarea);
	}
}
