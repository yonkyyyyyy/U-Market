package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.StockUpdateEventDTO;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ProductoRepository;
import com.uMarket.uMarket.repository.UsuarioRepository;
import com.uMarket.uMarket.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@RecordApplicationEvents
class ConcurrenciaWebSocketIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private ApplicationEvents applicationEvents;

	/**
	 * Simula N compradores concurrentes sobre un producto con stock=1.
	 * Se espera exactamente 1 HTTP 200 OK y el resto HTTP 409 Conflict.
	 */
	@Test
	void compraConcurrenteDevuelveUn200YResto409() throws Exception {
		// --- Arrange: vendedor y comprador ---
		Usuario vendedor = new Usuario();
		vendedor.setNombre("Vendedor");
		vendedor.setCorreoInstitucional("vendedor-conc@utp.edu.pe");
		vendedor.setPasswordHash("hash");
		vendedor.setRol("ESTUDIANTE");
		usuarioRepository.save(vendedor);

		Usuario comprador = new Usuario();
		comprador.setNombre("Comprador");
		comprador.setCorreoInstitucional("comprador-conc@utp.edu.pe");
		comprador.setPasswordHash("hash");
		comprador.setRol("ESTUDIANTE");
		usuarioRepository.save(comprador);

		// --- Arrange: producto con stock=1 ---
		Producto producto = new Producto();
		producto.setUsuario(vendedor);
		producto.setTitulo("Producto concurrente");
		producto.setPrecio(new BigDecimal("10.00"));
		producto.setEstado("ACTIVO");
		producto.setStock(1);
		productoRepository.save(producto);
		Long productoId = producto.getId();

		String token = jwtService.generateToken(comprador);

		int N = 10;
		ExecutorService executor = Executors.newFixedThreadPool(N);
		List<Callable<Integer>> tasks = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			tasks.add(() -> {
				MvcResult result = mockMvc.perform(post("/api/productos/" + productoId + "/comprar")
								.param("cantidad", "1")
								.header("Authorization", "Bearer " + token))
						.andReturn();
				return result.getResponse().getStatus();
			});
		}

		List<Future<Integer>> futures = executor.invokeAll(tasks);
		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> f : futures) {
			statuses.add(f.get());
		}
		executor.shutdown();

		long ok = statuses.stream().filter(s -> s == 200).count();
		long conflict = statuses.stream().filter(s -> s == 409).count();
		assertThat(ok).isEqualTo(1);
		assertThat(conflict).isEqualTo(N - 1);
	}

	/**
	 * Valida que tras una compra exitosa se emita el evento de stock.
	 */
	@Test
	void compraExitosaEmiteEventoDeStock() throws Exception {
		Usuario vendedor = new Usuario();
		vendedor.setNombre("Vendedor2");
		vendedor.setCorreoInstitucional("vendedor2-conc@utp.edu.pe");
		vendedor.setPasswordHash("hash");
		vendedor.setRol("ESTUDIANTE");
		usuarioRepository.save(vendedor);

		Usuario comprador = new Usuario();
		comprador.setNombre("Comprador2");
		comprador.setCorreoInstitucional("comprador2-conc@utp.edu.pe");
		comprador.setPasswordHash("hash");
		comprador.setRol("ESTUDIANTE");
		usuarioRepository.save(comprador);

		Producto producto = new Producto();
		producto.setUsuario(vendedor);
		producto.setTitulo("Producto evento");
		producto.setPrecio(new BigDecimal("20.00"));
		producto.setEstado("ACTIVO");
		producto.setStock(5);
		productoRepository.save(producto);

		String token = jwtService.generateToken(comprador);

		mockMvc.perform(post("/api/productos/" + producto.getId() + "/comprar")
						.param("cantidad", "1")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		long eventos = applicationEvents.stream(StockUpdateEventDTO.class).count();
		assertThat(eventos).isEqualTo(1);
	}
}
