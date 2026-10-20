package com.uMarket.uMarket.service;

import com.uMarket.uMarket.config.RabbitMQConfig;
import com.uMarket.uMarket.dto.TareaPesada;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Worker asíncrono: consume las tareas pesadas de la cola y ejecuta el pipeline
 * real de moderación. La concurrencia la define {@code spring.rabbitmq.listener.simple.*}
 * en application.yaml.
 *
 * <p>El procesamiento efectivo vive en {@link ProductoService} para reutilizar
 * los servicios de IA/Cloudinary y mantener la lógica en un solo lugar. Aquí
 * solo se enruta por tipo de tarea.</p>
 */
@Slf4j
@Component
public class TareaPesadaWorker {

	private final ProductoService productoService;

	public TareaPesadaWorker(ProductoService productoService) {
		this.productoService = productoService;
	}

	@RabbitListener(queues = RabbitMQConfig.COLA_MODERACION)
	public void procesar(TareaPesada tarea) {
		log.info("Worker recibió tarea {} (tipo={}, producto={}, archivo={})",
				tarea.id(), tarea.tipo(), tarea.productoId(), tarea.archivoId());

		if (TareaPesada.TIPO_IMAGEN.equalsIgnoreCase(tarea.tipo())) {
			// Modera la imagen y recalcula el estado del producto (activar/rechazar).
			productoService.procesarImagenPendiente(tarea.archivoId());
		} else if (TareaPesada.TIPO_PUBLICACION.equalsIgnoreCase(tarea.tipo())) {
			// Resuelve una publicación nueva: la activa si no tiene nada pendiente
			// (p. ej. sin imágenes) o la mantiene PENDIENTE si aún hay imágenes por moderar.
			productoService.recalcularEstado(tarea.productoId());
		} else {
			log.warn("Tipo de tarea no soportado por el pipeline: {}", tarea.tipo());
		}

		log.info("Worker terminó tarea {} ({}).", tarea.id(), tarea.tipo());
	}
}
