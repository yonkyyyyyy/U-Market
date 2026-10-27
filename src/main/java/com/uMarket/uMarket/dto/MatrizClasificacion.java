package com.uMarket.uMarket.dto;

/**
 * Matriz de clasificación de la IA de moderación, calculada en vivo a partir de
 * la revisión humana de los logs ({@code moderacion_logs}).
 *
 * <p>La clase "positiva" es <b>contenido prohibido</b>:</p>
 * <ul>
 *   <li>VP (verdadero positivo): la IA lo RECHAZÓ y el humano confirmó que era prohibido.</li>
 *   <li>VN (verdadero negativo): la IA lo APROBÓ y el humano confirmó que estaba bien.</li>
 *   <li>FP (falso positivo): la IA lo RECHAZÓ pero el humano dice que era válido.</li>
 *   <li>FN (falso negativo): la IA lo APROBÓ pero el humano dice que era prohibido.</li>
 * </ul>
 *
 * <p>Los registros {@code FALLIDO} (la IA no respondió) se excluyen de la matriz:
 * no son una decisión de clasificación. {@code pendientesDeRevision} son los
 * registros medibles que todavía nadie ha revisado.</p>
 */
public record MatrizClasificacion(
        long verdaderoPositivo,
        long falsoPositivo,
        long falsoNegativo,
        long verdaderoNegativo,
        long revisados,
        long pendientesDeRevision,
        long fallidos,
        long total,
        double exactitud,
        double precision,
        double recall,
        double f1
) {

    /** Arma la matriz y deriva las métricas estándar (0 cuando no hay datos). */
    public static MatrizClasificacion de(long vp, long fp, long fn, long vn,
                                         long pendientes, long fallidos, long total) {
        long revisados = vp + fp + fn + vn;
        double precision = dividir(vp, vp + fp);
        double recall = dividir(vp, vp + fn);
        double exactitud = dividir(vp + vn, revisados);
        double f1 = (precision + recall) == 0 ? 0 : (2 * precision * recall) / (precision + recall);
        return new MatrizClasificacion(vp, fp, fn, vn, revisados, pendientes, fallidos, total,
                redondear(exactitud), redondear(precision), redondear(recall), redondear(f1));
    }

    private static double dividir(long numerador, long denominador) {
        return denominador == 0 ? 0 : (double) numerador / denominador;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10_000d) / 10_000d;
    }
}
