package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.enums.ModerationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Evaluación del modelo de detección de sustancias prohibidas en IMÁGENES
 * (Tarea IA: Visión).
 *
 * <p>Instrumenta el "afinamiento": se valida el modelo multimodal elegido
 * ({@code gpt-4o-mini}) contra un set de fotos reales y se computan accuracy,
 * precisión, recall y F1. Si el F1 es bajo, se ajusta el umbral en
 * {@code application.yaml (openai.vision-umbral)} y se re-corre el test.</p>
 *
 * <p><b>Set de fotos:</b> se leen de
 * {@code src/test/resources/vision/prohibidas/} (fotos con drogas, armas, etc.)
 * y {@code src/test/resources/vision/permitidas/} (productos normales). Si las
 * carpetas no existen o están vacías, el test se salta.</p>
 *
 * <p><b>Requisito:</b> variable de entorno {@code OPENAI_API_KEY}. Si no está
 * configurada, el test se salta (no rompe {@code mvn test} en una PC sin key).</p>
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class VisionEvaluacionTest {

	private final VisionAnalisisService service = new VisionAnalisisService(
			RestClient.builder()
					.baseUrl("https://api.openai.com")
					.defaultHeader("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
					.defaultHeader("Content-Type", "application/json")
					.build(),
			"gpt-4o-mini",
			0.5,
			new ObjectMapper());

	/** Carpeta con el set de evaluación (relativa a la raíz del proyecto). */
	private static final Path BASE = Path.of("src", "test", "resources", "vision");

	/** Ejemplo del set: la foto y si realmente contiene una sustancia prohibida. */
	private record Ejemplo(File imagen, boolean prohibida) {
	}

	@Test
	void evaluarDeteccionDeSustanciasProhibidas() {
		List<Ejemplo> set = cargarSet();
		assumeTrue(!set.isEmpty(),
				"Agrega fotos en src/test/resources/vision/prohibidas/ y src/test/resources/vision/permitidas/");

		int tp = 0; // true positives: prohibida detectada como prohibida
		int tn = 0; // true negatives: permitida detectada como permitida
		int fp = 0; // false positives: permitida detectada como prohibida
		int fn = 0; // false negatives: prohibida detectada como permitida
		int fallidos = 0; // llamadas a las que la API no respondió (fail-open)

		for (Ejemplo ejemplo : set) {
			ModerationResultDTO resultado = service.analizar(ejemplo.imagen());

			if (resultado.getStatus() == ModerationStatus.FAILED) {
				fallidos++;
				System.out.printf(Locale.ROOT, "%-16s esperado=%-5s predicho=API-FALLIDA (fail-open) imagen='%s'%n",
						resultado.getStatus(), ejemplo.prohibida(), ejemplo.imagen().getName());
				continue;
			}

			boolean predicho = resultado.getStatus() == ModerationStatus.REJECTED;
			System.out.printf(Locale.ROOT, "%-16s esperado=%-5s predicho=%-5s conf=%.3f cats=%s imagen='%s'%n",
					resultado.getStatus(), ejemplo.prohibida(), predicho,
					resultado.getConfidenceScore(), resultado.getCategoriasDetectadas(),
					ejemplo.imagen().getName());

			if (predicho && ejemplo.prohibida()) tp++;
			else if (!predicho && !ejemplo.prohibida()) tn++;
			else if (predicho && !ejemplo.prohibida()) fp++;
			else fn++;
		}

		// Si la API falló en demasiadas llamadas (rate-limit de la key de prueba),
		// el resultado NO refleja la calidad del modelo: abortar con mensaje claro.
		int evaluados = tp + tn + fp + fn;
		assertThat(evaluados)
				.as("Al menos 90%% de las llamadas deben responder para medir el modelo " +
						"(de %d imágenes, %d respondieron, %d fallaron por la API)", set.size(), evaluados, fallidos)
				.isGreaterThan((int) (set.size() * 0.9));

		double total = tp + tn + fp + fn;
		double accuracy = (tp + tn) / total;
		double precision = tp == 0 ? 0.0 : (double) tp / (tp + fp);
		double recall = tp == 0 ? 0.0 : (double) tp / (tp + fn);
		double f1 = (precision + recall) == 0 ? 0.0 : 2 * (precision * recall) / (precision + recall);

		System.out.printf(Locale.ROOT, "%n== Métricas (set de %d imágenes, %d evaluadas, %d fallos de API) ==%n",
				set.size(), evaluados, fallidos);
		System.out.printf(Locale.ROOT, "TP=%d  TN=%d  FP=%d  FN=%d%n", tp, tn, fp, fn);
		System.out.printf(Locale.ROOT, "Accuracy = %.1f%%%n", accuracy * 100);
		System.out.printf(Locale.ROOT, "Precision = %.1f%%%n", precision * 100);
		System.out.printf(Locale.ROOT, "Recall   = %.1f%%%n", recall * 100);
		System.out.printf(Locale.ROOT, "F1 score = %.3f%n", f1);

		// Umbral mínimo aceptable: el modelo debe acertar al menos el 90% de las imágenes.
		assertThat(accuracy)
				.as("Accuracy >= 90%% sobre el set de evaluación")
				.isGreaterThanOrEqualTo(0.9);
	}

	private List<Ejemplo> cargarSet() {
		List<Ejemplo> set = new ArrayList<>();
		set.addAll(cargarCarpeta(BASE.resolve("prohibidas"), true));
		set.addAll(cargarCarpeta(BASE.resolve("permitidas"), false));
		return set;
	}

	private List<Ejemplo> cargarCarpeta(Path carpeta, boolean prohibida) {
		if (!Files.isDirectory(carpeta)) {
			return List.of();
		}
		try (var archivos = Files.list(carpeta)) {
			return archivos
					.filter(Files::isRegularFile)
					.filter(p -> esImagen(p.getFileName().toString()))
					.map(p -> new Ejemplo(p.toFile(), prohibida))
					.toList();
		} catch (Exception e) {
			return List.of();
		}
	}

	private boolean esImagen(String nombre) {
		String n = nombre.toLowerCase(Locale.ROOT);
		return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
	}
}
