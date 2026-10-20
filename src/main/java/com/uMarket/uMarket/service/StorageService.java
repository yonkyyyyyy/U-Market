package com.uMarket.uMarket.service;

import java.io.File;

/**
 * Almacenamiento temporal donde viven las imágenes MIENTRAS el pipeline
 * asíncrono las modera, para que no lleguen a Cloudinary hasta aprobarse.
 *
 * <p>Se define como interfaz para poder cambiar de implementación (disco local,
 * base de datos, MinIO…) sin tocar el pipeline.</p>
 */
public interface StorageService {

    /**
     * Guarda una copia de {@code archivo} y devuelve su identificador dentro
     * del almacén. El identificador conserva la extensión original (para que la
     * detección de MIME siga funcionando).
     */
    String guardar(File archivo);

    /** Devuelve la referencia al archivo guardado (aunque no exista todavía). */
    File leer(String id);

    /** Borra el archivo. No falla si ya no existe. */
    void borrar(String id);
}
