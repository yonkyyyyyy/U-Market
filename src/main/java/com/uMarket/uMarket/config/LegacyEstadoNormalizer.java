package com.uMarket.uMarket.config;

import com.uMarket.uMarket.repository.ProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migra al arrancar las publicaciones guardadas con la nomenclatura anterior del
 * estado publicado ('DISPONIBLE') al valor vigente ('ACTIVO').
 *
 * <p>Es idempotente y no destructivo: solo cambia la etiqueta del estado, así que
 * las publicaciones existentes siguen siendo visibles en el catálogo y las
 * búsquedas de sugerencias (que filtran por {@code ACTIVO}) las siguen encontrando.</p>
 */
@Slf4j
@Component
public class LegacyEstadoNormalizer implements ApplicationRunner {

    private final ProductoRepository productoRepository;

    public LegacyEstadoNormalizer(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migradas = productoRepository.normalizarEstadoLegacy();
        if (migradas > 0) {
            log.info("Estados normalizados: {} publicación(es) 'DISPONIBLE' -> 'ACTIVO'", migradas);
        }
    }
}
