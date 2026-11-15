package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.BusquedaChatResponse;
import com.uMarket.uMarket.dto.BusquedaNlpResponse;
import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la búsqueda NLP. No tocan la red: el {@code RestClient}
 * se mockea y la base se reemplaza por un repositorio Mockito.
 */
class BusquedaNlpServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ProductoRepository productoRepository;

	@BeforeEach
	void setUp() {
		productoRepository = mock(ProductoRepository.class);
	}

	private BusquedaNlpService servicio(RestClient restClient, boolean llmHabilitada, String apiKey) {
		return new BusquedaNlpService(restClient, productoRepository, objectMapper,
				"gpt-4o-mini", llmHabilitada, apiKey, 100);
	}

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

	private void candidatos(Producto... productos) {
		when(productoRepository.buscarCandidatosNlp(
				org.mockito.ArgumentMatchers.<Specification<Producto>>any(), any(Pageable.class)))
				.thenReturn(List.of(productos));
	}

	@Test
	void interpretaConReglasYConsultaCandidatos() {
		candidatos(producto(1L, "Laptop Lenovo", "Ideal para clases", "Tecnología", "Lima Este", "1200"));

		BusquedaNlpResponse respuesta = servicio(mock(RestClient.class), false, "")
				.buscar("busco laptop para clases en Lima Este hasta 1500 soles");

		assertThat(respuesta.interpretacion().categoria()).isEqualTo("Tecnología");
		assertThat(respuesta.interpretacion().campus()).isEqualTo("Lima Este");
		assertThat(respuesta.interpretacion().precioMaximo()).isEqualByComparingTo("1500");
		assertThat(respuesta.interpretacion().palabrasClave()).contains("laptop");
		assertThat(respuesta.interpretacion().origen()).isEqualTo("HEURISTICA");
		assertThat(respuesta.resultados()).extracting(ProductoDto::titulo)
				.containsExactly("Laptop Lenovo");
	}

	@Test
	void usaLaInterpretacionDelLlmCuandoEstaDisponible() {
		candidatos(producto(1L, "Laptop Lenovo", "Ideal para clases", "Tecnología", "Lima Este", "1200"));

		ApiMock api = apiMock();
		String json = "{\"palabrasClave\":[\"laptop\"],\"categoria\":\"Tecnologia\","
				+ "\"campus\":\"Lima Este\",\"precioMaximo\":1300,\"tags\":[]}";
		when(api.responseSpec().body(BusquedaChatResponse.class))
				.thenReturn(new BusquedaChatResponse("id", "gpt-4o-mini",
						List.of(new BusquedaChatResponse.Choice(0,
								new BusquedaChatResponse.Message("assistant", json), "stop"))));

		BusquedaNlpResponse respuesta = servicio(api.restClient(), true, "sk-fake")
				.buscar("necesito una laptop");

		assertThat(respuesta.interpretacion().origen()).isEqualTo("LLM");
		assertThat(respuesta.interpretacion().categoria()).isEqualTo("Tecnología");
		assertThat(respuesta.interpretacion().precioMaximo()).isEqualByComparingTo("1300");
		assertThat(respuesta.resultados()).extracting(ProductoDto::titulo)
				.containsExactly("Laptop Lenovo");
	}

	@Test
	void usaElParserHeuristicoSiElLlmFalla() {
		candidatos(
				producto(1L, "Laptop Lenovo", "Ideal para clases", "Tecnología", "Lima Este", "1200"));

		ApiMock api = apiMock();
		when(api.responseSpec().body(BusquedaChatResponse.class))
				.thenThrow(new RestClientException("401 Unauthorized"));

		BusquedaNlpResponse respuesta = servicio(api.restClient(), true, "sk-invalida")
				.buscar("laptop en Lima Este");

		assertThat(respuesta.interpretacion().origen()).isEqualTo("HEURISTICA");
		assertThat(respuesta.interpretacion().campus()).isEqualTo("Lima Este");
		assertThat(respuesta.resultados()).extracting(ProductoDto::titulo)
				.containsExactly("Laptop Lenovo");
	}

	@Test
	void ordenaPorRelevanciaLosCandidatosQueCoinciden() {
		candidatos(
				producto(1L, "Calculadora básica", "Modelo sencillo", "Tecnología", "Lima Este", "30"),
				producto(2L, "Calculadora científica Casio", "Modelo Casio", "Tecnología", "Lima Este", "80"));

		BusquedaNlpResponse respuesta = servicio(mock(RestClient.class), false, "")
				.buscar("calculadora casio científica");

		assertThat(respuesta.resultados()).extracting(ProductoDto::titulo)
				.containsExactly("Calculadora científica Casio", "Calculadora básica");
	}

	@Test
	void consultaVaciaNoConsultaLaBase() {
		BusquedaNlpResponse respuesta = servicio(mock(RestClient.class), true, "sk-fake").buscar("   ");

		assertThat(respuesta.interpretacion().vacia()).isTrue();
		assertThat(respuesta.resultados()).isEmpty();
		verifyNoInteractions(productoRepository);
	}

	private Producto producto(Long id, String titulo, String descripcion, String categoria,
			String campus, String precio) {
		Usuario vendedor = new Usuario();
		vendedor.setId(99L);
		vendedor.setNombre("Ana Torres");

		Producto producto = new Producto();
		producto.setId(id);
		producto.setUsuario(vendedor);
		producto.setTitulo(titulo);
		producto.setDescripcion(descripcion);
		producto.setCategoria(categoria);
		producto.setCampus(campus);
		producto.setPrecio(new BigDecimal(precio));
		producto.setEstado("ACTIVO");
		producto.setCreatedAt(LocalDateTime.now());
		return producto;
	}
}
