package com.uMarket.uMarket.dto;

import java.util.UUID;

/**
 * Tarea pesada encolada para procesamiento asíncrono.
 *
 * @param id         identificador único de la tarea
 * @param tipo       tipo de tarea: IMAGEN, TEXTO, OTRO. En las tareas 2 y 3
 *                   se usará para enrutar el análisis NLP / visión.
 * @param productoId id del producto asociado (opcional)
 * @param archivoId  id del archivo multimedia asociado (opcional)
 * @param referencia URL de la imagen o el texto a analizar
 */
public record TareaPesada(
        UUID id,
        String tipo,
        Long productoId,
        Long archivoId,
        String referencia
) {

    public static TareaPesada nueva(String tipo, Long productoId, Long archivoId, String referencia) {
        return new TareaPesada(UUID.randomUUID(), tipo, productoId, archivoId, referencia);
    }
}