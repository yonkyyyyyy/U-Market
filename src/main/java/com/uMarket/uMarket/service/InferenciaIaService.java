package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.VeredictoIaResponse;
import com.uMarket.uMarket.enums.ModerationStatus;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Fachada de la API interna de inferencia de IA ({@code /api/ia/v1/**}).
 *
 * <p>Es la "API interna" en el sentido de contrato de código: un único punto que
 * unifica la moderación de texto ({@link OpenAIModeracionService}) y de imagen
 * ({@link VisionAnalisisService}), mide la latencia y —lo importante— <b>aplica
 * la política de fallo por tipo</b>, para que el llamador reciba una decisión
 * lista y no tenga que replicar la regla:</p>
 * <ul>
 *   <li><b>TEXTO — fail-open:</b> solo un {@code REJECTED} explícito bloquea. Si la
 *       IA no está disponible ({@code FAILED}) se permite (igual que al publicar).</li>
 *   <li><b>IMAGEN — fail-closed:</b> solo un {@code APPROVED} explícito permite.
 *       {@code REJECTED} y {@code FAILED} bloquean (igual que el pipeline).</li>
 * </ul>
 *
 * <p>No persiste nada: a diferencia del pipeline asíncrono, esta API solo responde
 * el veredicto (los logs del panel siguen viniendo de las publicaciones reales).</p>
 */
@Slf4j
@Service
public class InferenciaIaService {

	private final OpenAIModeracionService openAIModeracionService;
	private final VisionAnalisisService visionAnalisisService;
	private final String modeloTexto;
	private final String modeloVision;
	private final int textoMax;

	public InferenciaIaService(OpenAIModeracionService openAIModeracionService,
								VisionAnalisisService visionAnalisisService,
								@Value("${openai.model:omni-moderation-latest}") String modeloTexto,
								@Value("${openai.vision-model:gpt-4o-mini}") String modeloVision,
								@Value("${ia.limites.texto-max:5000}") int textoMax) {
		this.openAIModeracionService = openAIModeracionService;
		this.visionAnalisisService = visionAnalisisService;
		this.modeloTexto = modeloTexto;
		this.modeloVision = modeloVision;
		this.textoMax = textoMax;
	}

	/**
	 * Analiza un texto y devuelve el veredicto con la decisión ya aplicada
	 * (fail-open).
	 *
	 * @param texto texto a analizar (no vacío y dentro del máximo configurado)
	 * @throws IllegalArgumentException si el texto está vacío o supera el máximo
	 */
	public VeredictoIaResponse inferirTexto(String texto) {
		validarTexto(texto);

		long inicio = System.nanoTime();
		ModerationResultDTO resultado = openAIModeracionService.analizar(texto);
		long latenciaMs = latenciaMs(inicio);

		// Fail-open: si la IA falla (FAILED) NO se bloquea, igual que en el pipeline.
		boolean permitido = resultado.getStatus() != ModerationStatus.REJECTED;

		log.info("API IA texto: veredicto={}, permitido={}, score={}, latenciaMs={}",
				resultado.getStatus(), permitido, resultado.getConfidenceScore(), latenciaMs);
		return respuesta(resultado, TipoModeracion.TEXTO, modeloTexto, permitido, latenciaMs);
	}

	/**
	 * Analiza la imagen de un producto y devuelve el veredicto con la decisión ya
	 * aplicada (fail-closed).
	 *
	 * @param imagen archivo de imagen a analizar, nunca {@code null}
	 * @throws NullPointerException si la imagen es {@code null}
	 */
	public VeredictoIaResponse inferirImagen(File imagen) {
		Objects.requireNonNull(imagen, "imagen no puede ser null");

		long inicio = System.nanoTime();
		ModerationResultDTO resultado = visionAnalisisService.analizar(imagen);
		long latenciaMs = latenciaMs(inicio);

		// Fail-closed: si la IA falla (FAILED) NO se permite, igual que en el pipeline.
		boolean permitido = resultado.getStatus() == ModerationStatus.APPROVED;

		log.info("API IA imagen: veredicto={}, permitido={}, score={}, latenciaMs={}",
				resultado.getStatus(), permitido, resultado.getConfidenceScore(), latenciaMs);
		return respuesta(resultado, TipoModeracion.IMAGEN, modeloVision, permitido, latenciaMs);
	}

	private void validarTexto(String texto) {
		if (texto == null || texto.isBlank()) {
			throw new IllegalArgumentException("texto no puede estar vacío");
		}
		if (texto.length() > textoMax) {
			throw new IllegalArgumentException("texto supera el máximo permitido de " + textoMax + " caracteres");
		}
	}

	/** Arma la respuesta: veredicto crudo + decisión aplicada, sin reinterpretar nada. */
	private VeredictoIaResponse respuesta(ModerationResultDTO resultado, TipoModeracion tipo,
											String modelo, boolean permitido, long latenciaMs) {
		return new VeredictoIaResponse(
				VeredictoModeracion.desde(resultado.getStatus()),
				permitido,
				resultado.getConfidenceScore(),
				resultado.getCategoriasDetectadas(),
				modelo,
				tipo,
				latenciaMs,
				LocalDateTime.now());
	}

	private long latenciaMs(long inicioNanos) {
		return (System.nanoTime() - inicioNanos) / 1_000_000;
	}
}
