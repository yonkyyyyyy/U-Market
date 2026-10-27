package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.VisionAnalisisResponse;
import com.uMarket.uMarket.enums.ModerationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del servicio de visión: no tocan la red (el {@code RestClient}
 * se mockea). Verifican la interpretación del veredicto, el umbral y el manejo de fallos.
 */
class VisionAnalisisServiceTest {

	private static final double UMBRAL = 0.5;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private VisionAnalisisService servicio(RestClient restClient) {
		return new VisionAnalisisService(restClient, "gpt-4o-mini", UMBRAL, objectMapper);
	}

	/** RestClient mockeado + su ResponseSpec final, para stubbear el resultado de la API. */
	private record ApiMock(RestClient restClient, RestClient.ResponseSpec responseSpec) {
	}

	/** Mock de la cadena fluida de RestClient que termina devolviendo el ResponseSpec. */
	private ApiMock apiMock() {
		RestClient restClient = mock(RestClient.class);
		RestClient.RequestBodyUriSpec chain = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

		when(restClient.post()).thenReturn(chain);
		when(chain.uri(anyString())).thenReturn(chain);
		when(chain.body(any(Object.class))).thenReturn(bodySpec);
		when(bodySpec.retrieve()).thenReturn(responseSpec);
		return new ApiMock(restClient, responseSpec);
	}

	@Test
	void interpretarApruebaImagenNormal() {
		ModerationResultDTO resultado = servicio(mock(RestClient.class)).interpretar(
				"{\"prohibido\": false, \"confianza\": 0.05, \"categorias\": []}", "cuaderno.jpg");

		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.APPROVED);
		assertThat(resultado.getConfidenceScore()).isEqualTo(0.05);
		assertThat(resultado.getCategoriasDetectadas()).isEmpty();
	}

	@Test
	void interpretarRechazaImagenProhibida() {
		ModerationResultDTO resultado = servicio(mock(RestClient.class)).interpretar(
				"{\"prohibido\": true, \"confianza\": 0.95, \"categorias\": [\"droga\"]}", "droga.jpg");

		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.REJECTED);
		assertThat(resultado.getCategoriasDetectadas()).containsExactly("droga");
	}

	@Test
	void interpretarApruebaSiElModeloDiceNoProhibidoAunqueTengaAltaConfianza() {
		// Caso real de un laptop: prohibido=false con confianza 0.99.
		// La confianza NO implica rechazo por sí sola (solo si prohibido=true).
		ModerationResultDTO resultado = servicio(mock(RestClient.class)).interpretar(
				"{\"prohibido\": false, \"confianza\": 0.99, \"categorias\": []}", "laptop.jpg");

		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.APPROVED);
	}

	@Test
	void interpretarRechazaSiProhibidoYLaConfianzaAlcanzaElUmbral() {
		ModerationResultDTO resultado = servicio(mock(RestClient.class)).interpretar(
				"{\"prohibido\": true, \"confianza\": 0.7, \"categorias\": [\"arma\"]}", "arma.jpg");

		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.REJECTED);
	}

	@Test
	void interpretarApruebaSiProhibidoPeroLaConfianzaNoAlcanzaElUmbral() {
		// El umbral filtra los positivos poco seguros (menos falsos positivos).
		ModerationResultDTO resultado = servicio(mock(RestClient.class)).interpretar(
				"{\"prohibido\": true, \"confianza\": 0.3, \"categorias\": [\"droga\"]}", "dudosa.jpg");

		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.APPROVED);
	}

	@Test
	void analizarRechazaImagenProhibidaConRespuestaDeLaApi() throws Exception {
		ApiMock api = apiMock();
		VisionAnalisisResponse respuesta = new VisionAnalisisResponse("id", "gpt-4o-mini",
				List.of(new VisionAnalisisResponse.Choice(0,
						new VisionAnalisisResponse.Message("assistant",
								"{\"prohibido\": true, \"confianza\": 0.9, \"categorias\": [\"arma\"]}"),
						"stop")));
		when(api.responseSpec().body(VisionAnalisisResponse.class)).thenReturn(respuesta);

		ModerationResultDTO resultado = servicio(api.restClient()).analizar(imagenTemp());

		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.REJECTED);
		assertThat(resultado.getCategoriasDetectadas()).containsExactly("arma");
	}

	@Test
	void analizarDevuelveFailedSiLaApiFalla() throws Exception {
		ApiMock api = apiMock();
		when(api.responseSpec().body(VisionAnalisisResponse.class)).thenThrow(new RestClientException("timeout"));

		ModerationResultDTO resultado = servicio(api.restClient()).analizar(imagenTemp());

		// El servicio devuelve FAILED; el llamador (ProductoService) aplica fail-closed.
		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.FAILED);
	}

	@Test
	void analizarDevuelveFailedSiLaRespuestaVieneVacia() throws Exception {
		ApiMock api = apiMock();
		when(api.responseSpec().body(VisionAnalisisResponse.class)).thenReturn(null);

		ModerationResultDTO resultado = servicio(api.restClient()).analizar(imagenTemp());

		assertThat(resultado.getStatus()).isEqualTo(ModerationStatus.FAILED);
	}

	private File imagenTemp() throws Exception {
		File temp = File.createTempFile("vision_test", ".jpg");
		Files.write(temp.toPath(), new byte[]{1, 2, 3, 4});
		return temp;
	}
}
