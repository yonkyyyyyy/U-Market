package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.GeneradorCopysChatResponse;
import com.uMarket.uMarket.dto.GenerarCopyRequest;
import com.uMarket.uMarket.exception.GeneracionCopyException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GeneradorCopysServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private record ApiMock(RestClient restClient, RestClient.ResponseSpec responseSpec) {
	}

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

	private GenerarCopyRequest solicitud() {
		return new GenerarCopyRequest("Libro de cálculo", "Buen estado", new BigDecimal("25"),
				"Lima Este", "Instagram", "Cercano", "Estudiantes");
	}

	@Test
	void devuelveTresCopysDelModelo() {
		ApiMock api = apiMock();
		String json = "{\"copys\":[\"Opción uno\",\"Opción dos\",\"Opción tres\"]}";
		when(api.responseSpec().body(GeneradorCopysChatResponse.class))
				.thenReturn(new GeneradorCopysChatResponse(List.of(
						new GeneradorCopysChatResponse.Choice(
								new GeneradorCopysChatResponse.Message(json)))));

		var respuesta = new GeneradorCopysService(api.restClient(), objectMapper,
				"gpt-4o-mini", true, "sk-test").generar(solicitud());

		assertThat(respuesta.copys()).containsExactly("Opción uno", "Opción dos", "Opción tres");
	}

	@Test
	void informaSiLaIntegracionNoEstaConfiguradaSinLlamarAlProveedor() {
		RestClient restClient = mock(RestClient.class);
		GeneradorCopysService service = new GeneradorCopysService(restClient, objectMapper,
				"gpt-4o-mini", true, "");

		assertThatThrownBy(() -> service.generar(solicitud()))
				.isInstanceOf(GeneracionCopyException.class)
				.hasMessageContaining("no está configurado");
		verifyNoInteractions(restClient);
	}

	@Test
	void noDevuelveUnaRespuestaConFormatoInvalido() {
		ApiMock api = apiMock();
		when(api.responseSpec().body(GeneradorCopysChatResponse.class))
				.thenReturn(new GeneradorCopysChatResponse(List.of(
						new GeneradorCopysChatResponse.Choice(
								new GeneradorCopysChatResponse.Message("{\"copys\":[\"solo uno\"]}")))));

		assertThatThrownBy(() -> new GeneradorCopysService(api.restClient(), objectMapper,
				"gpt-4o-mini", true, "sk-test").generar(solicitud()))
				.isInstanceOf(GeneracionCopyException.class)
				.hasMessageContaining("No se pudieron generar");
	}

	@Test
	void informaFalloDelProveedorEnLugarDeEntregarContenidoFalso() {
		ApiMock api = apiMock();
		when(api.responseSpec().body(GeneradorCopysChatResponse.class))
				.thenThrow(new RestClientException("timeout"));

		assertThatThrownBy(() -> new GeneradorCopysService(api.restClient(), objectMapper,
				"gpt-4o-mini", true, "sk-test").generar(solicitud()))
				.isInstanceOf(GeneracionCopyException.class)
				.hasMessageContaining("Inténtalo de nuevo más tarde");
	}
}
