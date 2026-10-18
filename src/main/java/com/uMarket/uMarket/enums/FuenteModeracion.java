package com.uMarket.uMarket.enums;

/**
 * De dónde salió la decisión registrada. Permite distinguir en el panel de
 * moderación las decisiones "reales" (las que tomó la IA sobre contenido del
 * usuario) de las administrativas (limpieza por TTL).
 */
public enum FuenteModeracion {
    PUBLICACION, // el texto de la publicación (crear/editar)
    IMAGEN,      // el pipeline asíncrono de imágenes
    TTL          // limpieza de staging: la imagen caducó sin respuesta de la IA
}
