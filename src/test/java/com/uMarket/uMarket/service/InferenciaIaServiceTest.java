package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.VeredictoIaResponse;
import com.uMarket.uMarket.enums.ModerationStatus;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de la fachada de la API interna: verifican la política de fallo
 * por tipo, la traducción del veredicto y las validaciones. No tocan la red (los
 * servicios de IA se mockean).
 */
class InferenciaIaServiceTest {

	private static final String MODELO_TEXTO = "omni-moderation-latest";
	private static final String MODELO_VISION = "gpt-4o-mini";
	private static final int TEXTO_MAX = 50;

	private final OpenAIModeracionService openAIModeracionService = mock(OpenAIModeracionService.class);

	private final VisionAnalisisService visionAnalisisService = mock(VisionAnalisisService.class);

	private InferenciaIaService servicio() {
		return new InferenciaIaService(openAIModeracionService, visionAnalisisService,
				MODELO_TEXTO, MODELO_VISION, TEXTO_MAX);
	}

	// --- Texto -----------------------------------------------------------------

	@Test
	void textoAprobadoSePermite() {
		when(openAIModeracionService.analizar(anyString()))
				.thenReturn(resultado(ModerationStatus.APPROVED, 0.01, List.of()));

		VeredictoIaResponse respuesta = servicio().inferirTexto("vendo un cuaderno");

		assertThat(respuesta.veredicto()).isEqualTo(VeredictoModeracion.APROBADO);
		assertThat(respuesta.permitido()).isTrue();
		assertThat(respuesta.tipo()).isEqualTo(TipoModeracion.TEXTO);
		assertThat(respuesta.modelo()).isEqualTo(MODELO_TEXTO);
		assertThat(respuesta.score()).isEqualTo(0.01);
		assertThat(respuesta.latenciaMs()).isGreaterThanOrEqualTo(0);
		assertThat(respuesta.evaluadoEn()).isNotNull();
	}

	@Test
	void textoRechazadoSeBloquea() {
		when(openAIModeracionService.analizar(anyString()))
				.thenReturn(resultado(ModerationStatus.REJECTED, 0.92, List.of("harassment")));

		VeredictoIaResponse respuesta = servicio().inferirTexto("texto inapropiado");

		assertThat(respuesta.veredicto()).isEqualTo(VeredictoModeracion.RECHAZADO);
		assertThat(respuesta.permitido()).isFalse();
		assertThat(respuesta.categorias()).containsExactly("harassment");
	}

	@Test
	void textoFallidoSePermitePorSerFailOpen() {
		when(openAIModeracionService.analizar(anyString()))
				.thenReturn(resultado(ModerationStatus.FAILED, 0.0, List.of()));

		VeredictoIaResponse respuesta = servicio().inferirTexto("cualquier texto");

		assertThat(respuesta.veredicto()).isEqualTo(VeredictoModeracion.FALLIDO);
		assertThat(respuesta.permitido()).isTrue();
	}

	@Test
	void textoVacioEsInvalido() {
		assertThatThrownBy(() -> servicio().inferirTexto("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("vacío");
	}

	@Test
	void textoDemasiadoLargoEsInvalido() {
		assertThatThrownBy(() -> servicio().inferirTexto("x".repeat(TEXTO_MAX + 1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("máximo");
	}

	@Test
	void textoConCategoriasNulasNoRompeLaRespuesta() {
		when(openAIModeracionService.analizar(anyString()))
				.thenReturn(resultado(ModerationStatus.APPROVED, 0.0, null));

		assertThat(servicio().inferirTexto("hola").categorias()).isEmpty();
	}

	// --- Imagen ----------------------------------------------------------------

	@Test
	void imagenAprobadaSePermite() {
		when(visionAnalisisService.analizar(any(File.class)))
				.thenReturn(resultado(ModerationStatus.APPROVED, 0.02, List.of()));

		VeredictoIaResponse respuesta = servicio().inferirImagen(new File("cuaderno.jpg"));

		assertThat(respuesta.veredicto()).isEqualTo(VeredictoModeracion.APROBADO);
		assertThat(respuesta.permitido()).isTrue();
		assertThat(respuesta.tipo()).isEqualTo(TipoModeracion.IMAGEN);
		assertThat(respuesta.modelo()).isEqualTo(MODELO_VISION);
	}

	@Test
	void imagenRechazadaSeBloquea() {
		when(visionAnalisisService.analizar(any(File.class)))
				.thenReturn(resultado(ModerationStatus.REJECTED, 0.95, List.of("droga")));

		VeredictoIaResponse respuesta = servicio().inferirImagen(new File("droga.jpg"));

		assertThat(respuesta.veredicto()).isEqualTo(VeredictoModeracion.RECHAZADO);
		assertThat(respuesta.permitido()).isFalse();
		assertThat(respuesta.categorias()).containsExactly("droga");
	}

	@Test
	void imagenFallidaSeBloqueaPorSerFailClosed() {
		when(visionAnalisisService.analizar(any(File.class)))
				.thenReturn(resultado(ModerationStatus.FAILED, 0.0, List.of()));

		VeredictoIaResponse respuesta = servicio().inferirImagen(new File("foto.jpg"));

		assertThat(respuesta.veredicto()).isEqualTo(VeredictoModeracion.FALLIDO);
		assertThat(respuesta.permitido()).isFalse();
	}

	@Test
	void imagenNulaEsInvalida() {
		assertThatThrownBy(() -> servicio().inferirImagen(null))
				.isInstanceOf(NullPointerException.class);
	}

	private ModerationResultDTO resultado(ModerationStatus status, double score, List<String> categorias) {
		ModerationResultDTO resultado = new ModerationResultDTO();
		resultado.setStatus(status);
		resultado.setConfidenceScore(score);
		resultado.setCategoriasDetectadas(categorias);
		return resultado;
	}
}
