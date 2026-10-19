package com.uMarket.uMarket.enums;

/**
 * Qué se analizó en una decisión de moderación: el texto de la publicación
 * (título + descripción) o una imagen del producto.
 */
public enum TipoModeracion {
    TEXTO,   // moderación de título + descripción (OpenAIModeracionService)
    IMAGEN   // moderación visual de una imagen (VisionAnalisisService)
}
