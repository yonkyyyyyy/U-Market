package com.uMarket.uMarket.enums;

/**
 * Veredicto FINAL que quedó registrado para un contenido moderado.
 *
 * <p>Es distinto de {@link ModerationStatus} (que además tiene {@code PENDING}):
 * aquí solo se persiste un resultado ya resuelto. {@code FALLIDO} significa que
 * la IA no pudo decidir (timeout, API caída) y el pipeline aplicó fail-closed.</p>
 */
public enum VeredictoModeracion {
    APROBADO,  // el contenido pasó el filtro
    RECHAZADO, // el contenido fue bloqueado
    FALLIDO;   // la IA no estuvo disponible: no hay veredicto real

    /** Traduce el estado que devuelven los servicios de IA a veredicto persistible. */
    public static VeredictoModeracion desde(ModerationStatus status) {
        return switch (status) {
            case APPROVED -> APROBADO;
            case REJECTED -> RECHAZADO;
            // PENDING no debería llegar aquí (el pipeline nunca persiste un análisis a
            // medias); se trata como FALLIDO para que el panel no lo cuente como acierto.
            case PENDING, FAILED -> FALLIDO;
        };
    }
}
