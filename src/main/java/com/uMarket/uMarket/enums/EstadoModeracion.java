package com.uMarket.uMarket.enums;

/**
 * Estado de moderación de una imagen subida por el usuario mientras el pipeline
 * asíncrono la analiza ANTES de publicarla en Cloudinary.
 *
 * <p>Este es el estado de la IMAGEN en revisión, no el del producto
 * ({@code Producto.estado}), que se deriva de sus imágenes.</p>
 */
public enum EstadoModeracion {
    PENDIENTE,   // recibida y en cola; ningún worker la ha resuelto todavía
    APROBADO,    // pasó el filtro de visión → se subió a Cloudinary
    RECHAZADO    // no pasó el filtro (contenido prohibido) o la IA falló (fail-closed)
}
