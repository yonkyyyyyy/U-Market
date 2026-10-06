package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.OpenAIModeracionResponse;
import com.uMarket.uMarket.enums.ModerationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluación del modelo de detección de lenguaje inapropiado (Tarea IA: NLP).
 *
 * <p>Instrumenta el "afinamiento": se valida el modelo elegido
 * ({@code omni-moderation-latest}) contra un set de ejemplos en español y se
 * computan accuracy, precisión, recall y F1. Si el F1 es bajo, se ajusta el
 * umbral en {@code application.yaml (openai.umbral)} y se re-corre el test.</p>
 *
 * <p><b>Requisito:</b> variable de entorno {@code OPENAI_API_KEY}. Si no está
 * configurada, el test se salta (no rompe {@code mvn test} en una PC sin key).</p>
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class OpenAIModeracionEvaluacionTest {

	private final OpenAIModeracionService service = new OpenAIModeracionService(
			RestClient.builder()
					.baseUrl("https://api.openai.com")
					.defaultHeader("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
					.defaultHeader("Content-Type", "application/json")
					.build(),
			"omni-moderation-latest",
			0.5);

	/** Set de evaluación en español: texto → ¿contiene lenguaje inapropiado? */
	private record Ejemplo(String texto, boolean inapropiado) {}

	private static final List<Ejemplo> SET_EVALUACION = List.of(
			// ——— Negativos (lenguaje normal, debe permitirse) ———
			new Ejemplo("Vendo calculadora Casio fx-991 en perfecto estado", false),
			new Ejemplo("Me interesa el producto, ¿sigue disponible?", false),
			new Ejemplo("Entrego el lunes por el campus o centro de la ciudad", false),
			new Ejemplo("Es un cuaderno nuevo, sin uso, lo vendo barato", false),
			new Ejemplo("Acepto trueque por audífonos o parlante", false),
			new Ejemplo("El celular funciona perfecto, solo tiene un rayón", false),
			new Ejemplo("Vendo refrigeradora pequeña, ideal para departamento", false),
			new Ejemplo("Fotos reales del producto, por si quieres verlo", false),
			new Ejemplo("Soy de la facultad de sistemas, venta de confianza", false),
			new Ejemplo("Acepto entregas dentro del campus únicamente", false),
			// ——— Positivos (lenguaje inapropiado, debe rechazarse) ———
			new Ejemplo("vete a la mierda ladrón", true),
			new Ejemplo("eres un imbécil y tu producto es una porquería", true),
			new Ejemplo("os vais a enterar hijos de puta", true),
			new Ejemplo("esta basura solo la vendería un estúpido", true),
			new Ejemplo("producto de mierda para gente pendeja", true),
			new Ejemplo("te voy a partir la cara si no me vendes barato", true),
			new Ejemplo("callate idiota, nadie te preguntó nada", true),
			new Ejemplo("maldito ladrón, devuelve mi plata", true),
			new Ejemplo("que asco tu producto, eres un inútil", true),
			new Ejemplo("vende tu basura en otro lado, imbécil", true),
			new Ejemplo("no me robes o te denuncio, sinvergüenza", true),
			new Ejemplo("eres lo peor que le pasó a esta universidad, estúpida", true)
	);

	@Test
	void evaluarDeteccionDeLenguajeInapropiado() {
		int tp = 0; // true positives: inapropiado detectado como inapropiado
		int tn = 0; // true negatives: normal detectado como normal
		int fp = 0; // false positives: normal detectado como inapropiado
		int fn = 0; // false negatives: inapropiado detectado como normal
		int fallidos = 0; // llamadas a las que la API no respondió (fail-open)

		for (Ejemplo ejemplo : SET_EVALUACION) {
			ModerationResultDTO resultado = service.analizar(ejemplo.texto());

			if (resultado.getStatus() == ModerationStatus.FAILED) {
				fallidos++;
				System.out.printf(Locale.ROOT, "%-20s escpectado=%-5s predicho=API-FALLIDA (fail-open) texto='%s'%n",
						resultado.getStatus(), ejemplo.inapropiado(), ejemplo.texto());
				continue;
			}

			boolean predicho = resultado.getStatus() == ModerationStatus.REJECTED;
			System.out.printf(Locale.ROOT, "%-20s esperado=%-5s predicho=%-5s score=%.3f texto='%s'%n",
					resultado.getStatus(), ejemplo.inapropiado(), predicho,
					resultado.getConfidenceScore(), ejemplo.texto());

			if (predicho && ejemplo.inapropiado()) tp++;
			else if (!predicho && !ejemplo.inapropiado()) tn++;
			else if (predicho && !ejemplo.inapropiado()) fp++;
			else fn++;
		}

		// Si la API falló en demasiadas llamadas (rate-limit de la key de prueba),
		// el resultado NO refleja la calidad del modelo: abortar con mensaje claro.
		int evaluados = tp + tn + fp + fn;
		assertThat(evaluados)
				.as("Al menos 90%% de las llamadas deben responder para medir el modelo " +
						"(de %d llamadas, %d respondieron, %d fallaron por la API)", SET_EVALUACION.size(), evaluados, fallidos)
				.isGreaterThan((int) (SET_EVALUACION.size() * 0.9));

		double total = tp + tn + fp + fn;
		double accuracy = (tp + tn) / total;
		double precision = tp == 0 ? 0.0 : (double) tp / (tp + fp);
		double recall = tp == 0 ? 0.0 : (double) tp / (tp + fn);
		double f1 = (precision + recall) == 0 ? 0.0 : 2 * (precision * recall) / (precision + recall);

		System.out.printf(Locale.ROOT, "%n== Métricas (set de %d ejemplos, %d evaluados, %d fallos de API) ==%n",
				SET_EVALUACION.size(), evaluados, fallidos);
		System.out.printf(Locale.ROOT, "TP=%d  TN=%d  FP=%d  FN=%d%n", tp, tn, fp, fn);
		System.out.printf(Locale.ROOT, "Accuracy = %.1f%%%n", accuracy * 100);
		System.out.printf(Locale.ROOT, "Precision = %.1f%%%n", precision * 100);
		System.out.printf(Locale.ROOT, "Recall   = %.1f%%%n", recall * 100);
		System.out.printf(Locale.ROOT, "F1 score = %.3f%n", f1);

		// Umbral mínimo aceptable: el modelo debe acertar al menos el 90% de los evaluados.
		assertThat(accuracy)
				.as("Accuracy >= 90%% sobre el set de evaluación")
				.isGreaterThanOrEqualTo(0.9);
	}
}