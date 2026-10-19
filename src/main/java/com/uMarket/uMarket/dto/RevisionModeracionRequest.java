package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Revisión humana de un log de moderación (feedback para la matriz).
 *
 * @param aciertoHumano {@code true} si el humano confirma el veredicto de la IA;
 *                      {@code false} si lo corrige.
 * @param comentario    motivo opcional de la corrección.
 */
public record RevisionModeracionRequest(

        @NotNull(message = "Debes indicar si el veredicto de la IA fue correcto")
        Boolean aciertoHumano,

        @Size(max = 500, message = "El comentario no debe superar los 500 caracteres")
        String comentario
) {
}
