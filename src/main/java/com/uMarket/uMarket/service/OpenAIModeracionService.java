package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.OpenAIModeracionResponse;
import com.uMarket.uMarket.enums.ModerationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

	/**
	 * Capa dura (blacklist) de términos que se rechazan SIEMPRE, aunque OpenAI
	 * no los marque. Se compara por palabra completa (token) tras normalizar
	 * (minúsculas, sin acentos), para evitar falsos positivos por subcadenas.
	 */
	private final Set<String> palabrasProhibidas;

	public OpenAIModeracionService(RestClient openaiRestClient,
									@Value("${openai.model:omni-moderation-latest}") String model,
									@Value("${openai.umbral:0.5}") double umbral) {
		this(openaiRestClient, model, umbral, "");
	}

	public OpenAIModeracionService(RestClient openaiRestClient,
									@Value("${openai.model:omni-moderation-latest}") String model,
									@Value("${openai.umbral:0.5}") double umbral,
									@Value("${openai.blacklist:}") String blacklistCsv) {
		this.openaiRestClient = openaiRestClient;
		this.model = model;
		this.umbral = umbral;
		this.palabrasProhibidas = cargarBlacklist(blacklistCsv);
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

		// Capa dura: blacklist local (palabras que se rechazan siempre, sin depender
		// de OpenAI). Evita que términos sueltos que el modelo no marque pasen.
		String palabraDetectada = detectarPalabraProhibida(texto);
		if (palabraDetectada != null) {
			log.info("Moderación texto: RECHAZADO por blacklist local (palabra='{}'), texto='{}'",
					palabraDetectada, abreviar(texto));
			resultado.setStatus(ModerationStatus.REJECTED);
			resultado.setConfidenceScore(1.0);
			resultado.setCategoriasDetectadas(List.of("blacklist"));
			return resultado;
		}

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

	/**
	 * Conjunto de términos prohibidos por defecto (capa dura). Se puede ampliar o
	 * reemplazar vía la propiedad {@code openai.blacklist} (lista separada por
	 * comas). Mantener esta lista acotada a términos claramente ilícitos para
	 * evitar falsos positivos en contextos legítimos.
	 */
	private static final Set<String> BLACKLIST_POR_DEFECTO = Set.of(
			// Armas / violencia
			"pistola", "revolver", "revólver", "arma", "armas", "cuchillo", "machete",
			"bomba", "explosivo", "granada", "rifle", "escopeta",
			// Drogas / sustancias
			"droga", "drogas", "marihuana", "cannabis", "cocaina", "cocaína", "cocaine",
			"heroina", "heroína", "heroin", "lsd", "extasis", "éxtasis", "ecstasy",
			"metanfetamina", "anfetamina", "fentanilo", "crack", "porro", "pastilla",
			// Insultos / groserías
			"mierda", "imbécil", "imbecil", "idiota", "estúpido", "estupido", "pendejo",
			"cabrón", "cabron", "puta", "puto", "maricón", "maricon", "joder", "coño",
			"cono", "carajo", "gilipollas", "zorra", "cabrona"
	);

	private static Set<String> cargarBlacklist(String csv) {
		Set<String> base;
		if (csv == null || csv.isBlank()) {
			base = new LinkedHashSet<>(BLACKLIST_POR_DEFECTO);
		} else {
			base = Arrays.stream(csv.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		return base.stream()
				.map(OpenAIModeracionService::normalizar)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Devuelve la palabra prohibida detectada (normalizada) o {@code null} si no
	 * hay ninguna. Compara por token completo, no por subcadena.
	 */
	private String detectarPalabraProhibida(String texto) {
		if (texto == null || palabrasProhibidas.isEmpty()) {
			return null;
		}
		String normalizado = normalizar(texto);
		for (String token : normalizado.split("[^\\p{L}\\p{N}]+")) {
			if (token.isEmpty()) {
				continue;
			}
			if (palabrasProhibidas.contains(token)) {
				return token;
			}
		}
		return null;
	}

	private static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String nfd = Normalizer.normalize(texto, Normalizer.Form.NFD);
		// Elimina diacríticos (acentos) y pasa a minúsculas con Locale.ROOT.
		String sinAcentos = nfd.replaceAll("\\p{M}", "");
		return sinAcentos.toLowerCase(Locale.ROOT);
	}

	private String abreviar(String texto) {
		if (texto == null) {
			return "";
		}
		return texto.length() > 60 ? texto.substring(0, 57) + "..." : texto;
	}
}