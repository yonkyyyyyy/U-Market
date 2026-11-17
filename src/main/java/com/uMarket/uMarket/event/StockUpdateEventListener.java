package com.uMarket.uMarket.event;

import com.uMarket.uMarket.dto.StockUpdateEventDTO;
import com.uMarket.uMarket.service.StockEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StockUpdateEventListener {

	private final StockEventPublisher stockEventPublisher;
	public StockUpdateEventListener(StockEventPublisher stockEventPublisher) {
		this.stockEventPublisher = stockEventPublisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alConfirmarStock(StockUpdateEventDTO event) {
		stockEventPublisher.publicar(event);
	}
}
