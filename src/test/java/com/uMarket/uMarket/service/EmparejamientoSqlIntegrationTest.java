package com.uMarket.uMarket.service;

import com.uMarket.uMarket.event.AlertaInternaEvent;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.DemandaRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import com.uMarket.uMarket.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class EmparejamientoSqlIntegrationTest {

	@Autowired
	private DemandaRepository demandaRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	void buscaEnBaseSoloCandidatasYLimitaElCalculoDeRecomendaciones() {
		Usuario vendedor = usuarioRepository.saveAndFlush(usuario("vendedor"));
		Usuario compradorUno = usuarioRepository.saveAndFlush(usuario("comprador1"));
		Usuario compradorDos = usuarioRepository.saveAndFlush(usuario("comprador2"));
		Usuario compradorTres = usuarioRepository.saveAndFlush(usuario("comprador3"));

		Producto producto = new Producto();
		producto.setUsuario(vendedor);
		producto.setTitulo("Calculadora Casio");
		producto.setDescripcion("Científica");
		producto.setCategoria("TECNOLOGIA");
		producto.setCampus("Lima Este");
		producto.setPrecio(new BigDecimal("50.00"));
		producto.setEstado("ACTIVO");
		producto.setCreatedAt(LocalDateTime.now());
		producto = productoRepository.saveAndFlush(producto);

		Demanda antigua = demanda(compradorUno, "Calculadora para ingeniería", "TECNOLOGIA",
				LocalDateTime.now().minusDays(3));
		Demanda reciente = demanda(compradorDos, "Busco calculadora científica", "TECNOLOGIA",
				LocalDateTime.now().minusDays(1));
		Demanda masReciente = demanda(compradorTres, "Calculadora para clases", "TECNOLOGIA",
				LocalDateTime.now());
		demandaRepository.saveAllAndFlush(List.of(antigua, reciente, masReciente));

		Demanda noCoincidente = demanda(compradorUno, "Cama para dormitorio", "HOGAR",
				LocalDateTime.now());
		demandaRepository.saveAndFlush(noCoincidente);
		demandaRepository.saveAndFlush(demanda(vendedor, "Calculadora propia", "TECNOLOGIA",
				LocalDateTime.now()));

		EmparejamientoService servicio = new EmparejamientoService(
				demandaRepository, productoRepository, mock(ApplicationEventPublisher.class));
		ReflectionTestUtils.setField(servicio, "candidatosMax", 2);

		var resultado = servicio.buscarDemandasCompatiblesParaProducto(producto.getId(), vendedor);

		assertThat(resultado).hasSize(2);
		assertThat(resultado).extracting("id")
				.containsExactly(reciente.getId(), masReciente.getId());
	}

	private Demanda demanda(Usuario usuario, String titulo, String categoria, LocalDateTime fechaCreacion) {
		Demanda demanda = new Demanda();
		demanda.setUsuario(usuario);
		demanda.setTitulo(titulo);
		demanda.setCategoria(categoria);
		demanda.setDescripcion("");
		demanda.setCampus(null);
		demanda.setPresupuestoEstimado(new BigDecimal("100.00"));
		demanda.setTags(Set.of());
		demanda.setEstado("ACTIVA");
		demanda.setFechaCreacion(fechaCreacion);
		return demanda;
	}

	private Usuario usuario(String nombre) {
		Usuario usuario = new Usuario();
		usuario.setNombre(nombre);
		usuario.setCorreoInstitucional(nombre + "@utp.edu.pe");
		usuario.setPasswordHash("hash");
		usuario.setRol("ESTUDIANTE");
		return usuario;
	}
}
