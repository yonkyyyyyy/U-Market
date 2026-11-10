package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.StockUpdateEventDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida STOMP para cambios de stock. Difunde el evento al canal
 * público {@code /topic/productos/{productoId}}, al que se suscriben los
 * clientes interesados en ese producto.
 */
@Component
public class StockEventPublisher {

	static final String TOPIC_PREFIX = "/topic/productos/";

	private final SimpMessagingTemplate messagingTemplate;

	public StockEventPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void publicar(StockUpdateEventDTO event) {
		messagingTemplate.convertAndSend(TOPIC_PREFIX + event.productoId(), event);
	}
}
