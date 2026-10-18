package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.VisionAnalisisResponse;
import com.uMarket.uMarket.dto.VisionVeredicto;
import com.uMarket.uMarket.enums.ModerationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Servicio de análisis de IMÁGENES vía la API de OpenAI (POST /v1/chat/completions
 * con un modelo multimodal, por defecto {@code gpt-4o-mini}).
 *
 * <p>Detecta sustancias prohibidas (drogas, medicamentos controlados) y armas
 * en las fotos de los productos. Reutiliza el {@code RestClient} de
 * {@code openai.*} y la misma política de resultado que la moderación de texto:
 * APPROVED / REJECTED / FAILED.</p>
 *
 * <p><b>Política de fallo:</b> si OpenAI responde con error, timeout o un JSON
 * inesperado, se devuelve {@code FAILED} (no se lanza excepción) y el fallo queda
 * registrado en el log. El <b>llamador decide</b> la política: para las fotos,
 * {@code ProductoService} aplica <b>fail-closed</b> (rechaza salvo {@code APPROVED}).</p>
 */
@Slf4j
@Service
public class VisionAnalisisService {

	private static final String ENDPOINT_CHAT = "/v1/chat/completions";

	private static final String PROMPT = """
			Eres un moderador de contenido de un marketplace universitario.
			Analiza la imagen de este producto y determina si muestra sustancias
			prohibidas (drogas, estupefacientes, medicamentos controlados), armas
			u otros objetos ilegales que no deberían publicarse.
			Responde ÚNICAMENTE con un JSON válido con este formato exacto:
			{"prohibido": true, "confianza": 0.95, "categorias": ["droga"]}
			Reglas:
			- "prohibido": true si aparece alguna sustancia u objeto ilegal; false si es un producto normal.
			- "confianza": número entre 0 y 1 con tu seguridad en ESA decisión, no en
			  si es o no prohibido. Es alta tanto cuando dices que SÍ como cuando dices que NO.
			- "categorias": lista de las categorías detectadas, vacía si no hay ninguna
			  (valores posibles: "droga", "arma", "medicamento", "otro").
			""";

	private final RestClient openaiRestClient;

	private final String model;

	/** Umbral de confianza (0..1) para considerar la imagen REJECTED. */
	private final double umbral;

	private final ObjectMapper objectMapper;

	public VisionAnalisisService(RestClient openaiRestClient,
									@Value("${openai.vision-model:gpt-4o-mini}") String model,
									@Value("${openai.vision-umbral:0.5}") double umbral,
									ObjectMapper objectMapper) {
		this.openaiRestClient = openaiRestClient;
		this.model = model;
		this.umbral = umbral;
		this.objectMapper = objectMapper;
	}

	/**
	 * Analiza la foto de un producto y devuelve el resultado de moderación.
	 *
	 * @param imagen archivo de imagen ya comprimido/redimensionado, nunca {@code null}
	 * @return {@code ModerationResultDTO} con status APPROVED/REJECTED/FAILED
	 */
	public ModerationResultDTO analizar(File imagen) {
		Objects.requireNonNull(imagen, "imagen no puede ser null");

		ModerationResultDTO resultado = new ModerationResultDTO();

		try {
			String dataUrl = aDataUrl(imagen);

			Map<String, Object> cuerpo = Map.of(
					"model", model,
					"response_format", Map.of("type", "json_object"),
					"messages", List.of(Map.of(
							"role", "user",
							"content", List.of(
									Map.of("type", "text", "text", PROMPT),
									Map.of("type", "image_url",
											"image_url", Map.of("url", dataUrl))))));

			VisionAnalisisResponse response = openaiRestClient.post()
					.uri(ENDPOINT_CHAT)
					.body(cuerpo)
					.retrieve()
					.body(VisionAnalisisResponse.class);

			String contenido = response == null ? null : response.contenido();
			if (contenido == null || contenido.isBlank()) {
				log.warn("OpenAI devolvió respuesta vacía para la imagen: {}", imagen.getName());
				resultado.setStatus(ModerationStatus.FAILED);
				resultado.setConfidenceScore(0.0);
				return resultado;
			}

			return interpretar(contenido, imagen.getName());

		} catch (IOException e) {
			log.warn("No se pudo leer la imagen {}: {}", imagen.getName(), e.getMessage());
			resultado.setStatus(ModerationStatus.FAILED);
			resultado.setConfidenceScore(0.0);
		} catch (RuntimeException e) {
			// RestClientException (Spring) y JacksonException (Jackson 3) son unchecked.
			// Se devuelve FAILED; el llamador decide (ProductoService aplica fail-closed).
			log.warn("Error llamando a OpenAI para visión: {}", e.getMessage());
			resultado.setStatus(ModerationStatus.FAILED);
			resultado.setConfidenceScore(0.0);
		}
		return resultado;
	}

	/**
	 * Interpreta el JSON devuelto por el modelo y aplica el umbral de confianza.
	 * Se separa del acceso a red para poder probarlo de forma unitaria.
	 *
	 * <p><b>Regla de decisión:</b> se rechaza solo si el modelo afirma que la
	 * imagen es prohibida ({@code prohibido=true}) <b>y</b> su seguridad en esa
	 * decisión alcanza el umbral. La {@code confianza} es la seguridad en la
	 * decisión (alta tanto para "es droga" como para "no es droga"), por eso NO
	 * se usa sola para rechazar — si no, se rechazaría todo.</p>
	 *
	 * @param contenidoJson JSON del veredicto
	 * @param nombreImagen   nombre de la imagen (solo para el log)
	 */
	ModerationResultDTO interpretar(String contenidoJson, String nombreImagen) {
		ModerationResultDTO resultado = new ModerationResultDTO();
		VisionVeredicto veredicto = objectMapper.readValue(contenidoJson, VisionVeredicto.class);

		boolean rechazar = veredicto.prohibido() && veredicto.confianza() >= umbral;
		resultado.setStatus(rechazar ? ModerationStatus.REJECTED : ModerationStatus.APPROVED);
		resultado.setConfidenceScore(veredicto.confianza());
		resultado.setCategoriasDetectadas(veredicto.categorias());
		log.info("Visión: prohibido={}, confianza={}, rechazar={}, categorías={}, imagen='{}'",
				veredicto.prohibido(), veredicto.confianza(), rechazar, veredicto.categorias(), nombreImagen);
		return resultado;
	}

	public double getUmbral() {
		return umbral;
	}

	private String aDataUrl(File imagen) throws IOException {
		byte[] bytes = Files.readAllBytes(imagen.toPath());
		String base64 = Base64.getEncoder().encodeToString(bytes);
		return "data:" + tipoMime(imagen) + ";base64," + base64;
	}

	private String tipoMime(File imagen) {
		String nombre = imagen.getName().toLowerCase();
		if (nombre.endsWith(".png")) {
			return "image/png";
		}
		if (nombre.endsWith(".webp")) {
			return "image/webp";
		}
		if (nombre.endsWith(".gif")) {
			return "image/gif";
		}
		return "image/jpeg";
	}
}
