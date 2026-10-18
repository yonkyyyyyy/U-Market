package com.uMarket.uMarket.service;

import com.uMarket.uMarket.config.RabbitMQConfig;
import com.uMarket.uMarket.dto.TareaPesada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TareaPesadaProducerTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@AfterEach
	void limpiarTransaccion() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	void publicaDirectamenteSiNoHayTransaccion() {
		TareaPesadaProducer producer = new TareaPesadaProducer(rabbitTemplate);
		TareaPesada tarea = TareaPesada.imagen(1L, 2L, "uuid.jpg");

		producer.publicar(tarea);

		verify(rabbitTemplate).convertAndSend(
				RabbitMQConfig.EXCHANGE_TAREAS, RabbitMQConfig.ROUTING_MODERACION, tarea);
	}

	@Test
	void dentroDeUnaTransaccionNoPublicaHastaElCommit() {
		TareaPesadaProducer producer = new TareaPesadaProducer(rabbitTemplate);
		TareaPesada tarea = TareaPesada.imagen(1L, 2L, "uuid.jpg");

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);

		producer.publicar(tarea);

		// La fila aún no está commiteada: el mensaje NO debe haberse enviado.
		verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(TareaPesada.class));

		// Simula el COMMIT de la transacción.
		TransactionSynchronizationManager.getSynchronizations()
				.forEach(TransactionSynchronization::afterCommit);

		verify(rabbitTemplate).convertAndSend(
				RabbitMQConfig.EXCHANGE_TAREAS, RabbitMQConfig.ROUTING_MODERACION, tarea);
	}
}
