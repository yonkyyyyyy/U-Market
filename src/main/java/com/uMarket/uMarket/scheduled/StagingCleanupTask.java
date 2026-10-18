package com.uMarket.uMarket.scheduled;

import com.uMarket.uMarket.enums.EstadoModeracion;
import com.uMarket.uMarket.model.ImagenEnRevision;
import com.uMarket.uMarket.repository.ImagenEnRevisionRepository;
import com.uMarket.uMarket.service.ProductoService;
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
    private final long horasTtl;

    public StagingCleanupTask(ImagenEnRevisionRepository imagenEnRevisionRepository,
                              StorageService storageService,
                              ProductoService productoService,
                              @Value("${uploads.staging-ttl-horas:24}") long horasTtl) {
        this.imagenEnRevisionRepository = imagenEnRevisionRepository;
        this.storageService = storageService;
        this.productoService = productoService;
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
        }
        log.info("Limpieza de staging: {} imagen(es) en revisión antigua(s) marcada(s) como RECHAZADO", antiguas.size());
    }
}
