package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.TareaPesada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TareaPesadaWorkerTest {

	@Mock
	private ProductoService productoService;

	@Test
	void enrutaUnaImagenALaModeracion() {
		TareaPesadaWorker worker = new TareaPesadaWorker(productoService);

		worker.procesar(TareaPesada.imagen(1L, 7L, "uuid.jpg"));

		verify(productoService).procesarImagenPendiente(7L);
		verify(productoService, never()).recalcularEstado(anyLong());
	}

	@Test
	void enrutaUnaPublicacionAlRecalculoDeEstado() {
		TareaPesadaWorker worker = new TareaPesadaWorker(productoService);

		worker.procesar(TareaPesada.publicacion(1L));

		verify(productoService).recalcularEstado(1L);
		verify(productoService, never()).procesarImagenPendiente(anyLong());
	}
}
