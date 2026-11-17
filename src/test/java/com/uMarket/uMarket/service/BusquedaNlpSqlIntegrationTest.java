package com.uMarket.uMarket.service;

import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ProductoRepository;
import com.uMarket.uMarket.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class BusquedaNlpSqlIntegrationTest {

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	void convierteConsultaNaturalEnFiltrosEjecutadosPorLaBaseDeDatos() {
		Usuario vendedor = new Usuario();
		vendedor.setNombre("Ana Torres");
		vendedor.setCorreoInstitucional("ana@universidad.edu");
		vendedor.setPasswordHash("hash");
		vendedor = usuarioRepository.saveAndFlush(vendedor);

		productoRepository.save(producto(vendedor, "Comida casera", "Menú criollo", "7.00", "ACTIVO"));
		productoRepository.save(producto(vendedor, "Comida familiar", "Menú para compartir", "8.00", "ACTIVO"));
		productoRepository.save(producto(vendedor, "Cuaderno", "Para clases", "5.00", "ACTIVO"));
		productoRepository.save(producto(vendedor, "Comida delivery", "Menú del día", "5.00", "PAUSADO"));
		Producto libro = producto(vendedor, "Libro de cálculo", "Texto de matemáticas", "2.00", "ACTIVO");
		libro.setCategoria("Libros y apuntes");
		productoRepository.save(libro);
		productoRepository.flush();

		BusquedaNlpService servicio = new BusquedaNlpService(
				mock(RestClient.class), productoRepository, new ObjectMapper(),
				"gpt-4o-mini", false, "", 100);

		var respuesta = servicio.buscar("comida por 7 soles");

		assertThat(respuesta.interpretacion().palabrasClave()).contains("comida");
		assertThat(respuesta.interpretacion().precioMaximo()).isEqualByComparingTo("7");
		assertThat(respuesta.resultados()).extracting("titulo")
				.containsExactly("Comida casera");

		var respuestaConTildes = servicio.buscar("libro de calculo");
		assertThat(respuestaConTildes.resultados()).extracting("titulo")
				.containsExactly("Libro de cálculo");
	}

	private Producto producto(Usuario vendedor, String titulo, String descripcion, String precio, String estado) {
		Producto producto = new Producto();
		producto.setUsuario(vendedor);
		producto.setTitulo(titulo);
		producto.setDescripcion(descripcion);
		producto.setPrecio(new BigDecimal(precio));
		producto.setEstado(estado);
		producto.setCreatedAt(LocalDateTime.now());
		return producto;
	}
}
