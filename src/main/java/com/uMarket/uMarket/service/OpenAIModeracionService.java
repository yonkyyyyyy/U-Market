package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.OpenAIModeracionResponse;
import com.uMarket.uMarket.enums.ModerationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Servicio de moderación de texto vía la API de OpenAI (POST /v1/moderations).
 *
 * <p><b>Modelo:</b> {@code omni-moderation-latest} (multicategoría, multilenguaje,
 * incluye español). Sin modelo local, sin GPU — el cómputo ocurre en OpenAI.</p>
 *
 * <p><b>Política de fallo (fail-open):</b> si OpenAI responde con error o timeout,
 * se devuelve {@code FAILED}. El llamador decide: por defecto se permite publicar
 * (no se bloquea un contenido dudoso por un problema de infraestructura) y se
 * registra el fallo.</p>
 */
@Slf4j
@Service
public class OpenAIModeracionService {

	private static final String ENDPOINT_MODERATIONS = "/v1/moderations";

	private final RestClient openaiRestClient;

	private final String model;

	/** Umbral de score por categoría para considerar el contenido REJECTED (0..1). */
	private final double umbral;

	public OpenAIModeracionService(RestClient openaiRestClient,
									@Value("${openai.model:omni-moderation-latest}") String model,
									@Value("${openai.umbral:0.5}") double umbral) {
		this.openaiRestClient = openaiRestClient;
		this.model = model;
		this.umbral = umbral;
	}

	/**
	 * Analiza un texto (título + descripción de la publicación) y devuelve el
	 * resultado de moderación.
	 *
	 * @param texto texto a analizar, nunca {@code null}
	 * @return {@code ModerationResultDTO} con status APPROVED/REJECTED/FAILED
	 */
	public ModerationResultDTO analizar(String texto) {
		Objects.requireNonNull(texto, "texto no puede ser null");

		ModerationResultDTO resultado = new ModerationResultDTO();
		// La API de moderación no usa imagen: se dejan los campos de imagen vacíos.
		resultado.setImageId(null);
		resultado.setOriginalUrl(null);

		try {
			OpenAIModeracionResponse response = openaiRestClient.post()
					.uri(ENDPOINT_MODERATIONS)
					.body(Map.of("model", model, "input", texto))
					.retrieve()
					.body(OpenAIModeracionResponse.class);

			OpenAIModeracionResponse.OpenAIModeracionResult entry =
					response == null || response.results() == null || response.results().isEmpty()
							? null
							: response.results().get(0);

			if (entry == null) {
				log.warn("OpenAI devolvió respuesta vacía para el texto: {}", abreviar(texto));
				resultado.setStatus(ModerationStatus.FAILED);
				resultado.setConfidenceScore(0.0);
				return resultado;
			}

			double maxScore = entry.maxScore();
			List<String> detectadas = entry.categoriasDetectadas();
			boolean inapropiado = entry.flagged() || maxScore >= umbral;

			resultado.setStatus(inapropiado ? ModerationStatus.REJECTED : ModerationStatus.APPROVED);
			resultado.setConfidenceScore(maxScore);
			resultado.setCategoriasDetectadas(detectadas);
			log.info("Moderación texto: flagged={}, score={}, categorías={}, texto='{}'",
					entry.flagged(), maxScore, detectadas, abreviar(texto));

		} catch (RestClientException e) {
			// Fail-open: no bloquear publicación por error de infraestructura.
			log.warn("Error llamando a OpenAI para moderación: {}", e.getMessage());
			resultado.setStatus(ModerationStatus.FAILED);
			resultado.setConfidenceScore(0.0);
		}
		return resultado;
	}

	public double getUmbral() {
		return umbral;
	}

	private String abreviar(String texto) {
		if (texto == null) {
			return "";
		}
		return texto.length() > 60 ? texto.substring(0, 57) + "..." : texto;
	}
}