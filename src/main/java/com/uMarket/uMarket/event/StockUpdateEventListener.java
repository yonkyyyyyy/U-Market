package com.uMarket.uMarket.event;

import com.uMarket.uMarket.dto.StockUpdateEventDTO;
import com.uMarket.uMarket.service.StockEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Difunde el cambio de stock solo después del commit.
 *
 * <p>{@code AFTER_COMMIT} evita notificar a los clientes si la transacción de
 * compra falla o hace rollback. Si no hay transacción activa, el listener no
 * se ejecuta ({@code fallbackExecution} queda en {@code false}).</p>
 */
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
