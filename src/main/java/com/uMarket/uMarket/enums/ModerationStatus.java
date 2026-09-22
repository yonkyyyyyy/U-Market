package com.uMarket.uMarket.enums;

public enum ModerationStatus {
    PENDING,    // Recibido, pendiente de análisis
    APPROVED,   // Contenido permitido
    REJECTED,   // Contenido prohibido/inapropiado
    FAILED      // Error en el procesamiento
}
