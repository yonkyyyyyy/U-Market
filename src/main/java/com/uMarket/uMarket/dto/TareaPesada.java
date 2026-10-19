package com.uMarket.uMarket.dto;

import java.util.UUID;

/**
 * Tarea pesada encolada para procesamiento asíncrono.
 *
 * @param id         identificador único de la tarea
 * @param tipo       tipo de tarea ({@link #TIPO_PUBLICACION} o {@link #TIPO_IMAGEN});
 *                   se usa para enrutar el análisis dentro del worker.
 * @param productoId id del producto asociado
 * @param archivoId  id de la imagen en revisión ({@code ImagenEnRevision}) a moderar
 * @param referencia id del archivo en el staging (almacenamiento temporal)
 */
public record TareaPesada(
        UUID id,
        String tipo,
        Long productoId,
        Long archivoId,
        String referencia
) {

    /** El worker resuelve el estado inicial de una publicación recién creada. */
    public static final String TIPO_PUBLICACION = "PUBLICACION";

    /** El worker modera una imagen en revisión (visión + Cloudinary). */
    public static final String TIPO_IMAGEN = "IMAGEN";

    /** Tarea para que el pipeline resuelva (y active) una publicación nueva. */
    public static TareaPesada publicacion(Long productoId) {
        return new TareaPesada(UUID.randomUUID(), TIPO_PUBLICACION, productoId, null, null);
    }

    /** Tarea para moderar una imagen en revisión. */
    public static TareaPesada imagen(Long productoId, Long archivoId, String referencia) {
        return new TareaPesada(UUID.randomUUID(), TIPO_IMAGEN, productoId, archivoId, referencia);
    }
}
