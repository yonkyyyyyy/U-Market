package com.uMarket.uMarket.scheduled;

import com.uMarket.uMarket.enums.EstadoModeracion;
import com.uMarket.uMarket.model.ImagenEnRevision;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.repository.ImagenEnRevisionRepository;
import com.uMarket.uMarket.service.ProductoService;
import com.uMarket.uMarket.service.RegistroModeracionService;
import com.uMarket.uMarket.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Limpieza de imágenes que quedaron "en revisión" demasiado tiempo (por ejemplo,
 * porque el mensaje se perdió): borra el archivo de staging y marca la revisión
 * como {@code RECHAZADO}, para que el producto no quede {@code PENDIENTE} para siempre.
 */
@Slf4j
@Component
public class StagingCleanupTask {

    private final ImagenEnRevisionRepository imagenEnRevisionRepository;
    private final StorageService storageService;
    private final ProductoService productoService;
    private final RegistroModeracionService registroModeracionService;
    private final long horasTtl;

    public StagingCleanupTask(ImagenEnRevisionRepository imagenEnRevisionRepository,
                              StorageService storageService,
                              ProductoService productoService,
                              RegistroModeracionService registroModeracionService,
                              @Value("${uploads.staging-ttl-horas:24}") long horasTtl) {
        this.imagenEnRevisionRepository = imagenEnRevisionRepository;
        this.storageService = storageService;
        this.productoService = productoService;
        this.registroModeracionService = registroModeracionService;
        this.horasTtl = horasTtl;
    }

    @Scheduled(fixedDelayString = "${uploads.staging-cleanup-ms:3600000}",
            initialDelayString = "${uploads.staging-cleanup-ms:3600000}")
    @Transactional
    public void limpiarPendientes() {
        LocalDateTime limite = LocalDateTime.now().minusHours(horasTtl);
        List<ImagenEnRevision> antiguas = imagenEnRevisionRepository
                .findByEstadoModeracionAndCreatedAtBefore(EstadoModeracion.PENDIENTE, limite);

        if (antiguas.isEmpty()) {
            return;
        }

        for (ImagenEnRevision imagen : antiguas) {
            storageService.borrar(imagen.getStagingPath());
            imagen.setEstadoModeracion(EstadoModeracion.RECHAZADO);
            imagenEnRevisionRepository.save(imagen);
            productoService.recalcularEstado(imagen.getProducto().getId());
            registrarLog(imagen);
        }
        log.info("Limpieza de staging: {} imagen(es) en revisión antigua(s) marcada(s) como RECHAZADO", antiguas.size());
    }

    /**
     * Deja el rastro en el log de moderación. Estas imágenes no tienen veredicto de
     * la IA (nadie respondió), por eso quedan como FALLIDO y no ensucian la matriz.
     * Es best-effort: un fallo aquí no debe impedir la limpieza.
     */
    private void registrarLog(ImagenEnRevision imagen) {
        try {
            Producto producto = imagen.getProducto();
            registroModeracionService.registrarTtl(
                    producto.getId(), producto.getTitulo(), producto.getUsuario().getId());
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el log de moderación de la limpieza TTL: {}", e.getMessage());
        }
    }
}
