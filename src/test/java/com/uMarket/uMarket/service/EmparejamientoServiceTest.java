package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.event.AlertaInternaEvent;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.DemandaRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmparejamientoServiceTest {

	@Mock
	private DemandaRepository demandaRepository;

	@Mock
	private ProductoRepository productoRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private EmparejamientoService emparejamientoService;

	@BeforeEach
	void setUp() {
		emparejamientoService = new EmparejamientoService(demandaRepository, productoRepository, eventPublisher);
	}

	@Test
	void elScoreSumaCampusYEtiquetas() {
		Demanda demanda = demanda(1L, usuario(2L), "Calculadora científica", "TECNOLOGIA",
				"Lima Este", Set.of("casio"));
		Producto producto = producto(10L, usuario(1L), "Calculadora Casio", "TECNOLOGIA", "Lima Este");

		double score = emparejamientoService.calcularScore(demanda, producto);

		assertThat(score).isGreaterThan(0.8);
	}

	@Test
	void buscaDemandasActivasCompatiblesParaElDuenoDelProducto() {
		Usuario vendedor = usuario(1L);
		Producto producto = producto(10L, vendedor, "Calculadora Casio", "TECNOLOGIA", "Lima Este");
		Demanda compatible = demanda(1L, usuario(2L), "Calculadora científica", "TECNOLOGIA",
				"Lima Este", Set.of("casio"));
		Demanda incompatible = demanda(2L, usuario(3L), "Cama de una plaza", "HOGAR",
				"Piura", Set.of());
		incompatible.setDescripcion("Cama de madera para dormitorio");
		incompatible.setPresupuestoEstimado(new BigDecimal("1.00"));
		incompatible.setFechaCreacion(LocalDateTime.now().minusYears(2));

		when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
		when(demandaRepository.buscarCandidatasParaProducto(
				eq("TECNOLOGIA"), eq("calculadora"), eq("Lima Este"), eq(1L), any(Pageable.class)))
				.thenReturn(List.of(compatible, incompatible));

		List<DemandaResponseDTO> resultado = emparejamientoService
				.buscarDemandasCompatiblesParaProducto(10L, vendedor);

		assertThat(resultado).extracting(DemandaResponseDTO::id).containsExactly(1L);
	}

	@Test
	void soloElDuenoPuedeVerLasDemandasCompatibles() {
		Producto producto = producto(10L, usuario(1L), "Calculadora Casio", "TECNOLOGIA", "Lima Este");
		when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

		assertThatThrownBy(() -> emparejamientoService.buscarDemandasCompatiblesParaProducto(10L, usuario(2L)))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void notificaAlAutorDeLaDemandaCompatible() {
		Producto producto = producto(10L, usuario(1L), "Calculadora Casio", "TECNOLOGIA", "Lima Este");
		Demanda compatible = demanda(1L, usuario(2L), "Calculadora científica", "TECNOLOGIA",
				"Lima Este", Set.of("casio"));

		when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
		when(demandaRepository.buscarCandidatasParaProducto(
				eq("TECNOLOGIA"), eq("calculadora"), eq("Lima Este"), eq(1L), any(Pageable.class)))
				.thenReturn(List.of(compatible));

		emparejamientoService.notificarDemandasCompatibles(10L);

		verify(eventPublisher).publishEvent(any(AlertaInternaEvent.class));
	}

	private Demanda demanda(Long id, Usuario usuario, String titulo, String categoria,
			String campus, Set<String> tags) {
		Demanda demanda = new Demanda();
		demanda.setId(id);
		demanda.setUsuario(usuario);
		demanda.setTitulo(titulo);
		demanda.setDescripcion("Modelo Casio fx-991");
		demanda.setCategoria(categoria);
		demanda.setCampus(campus);
		demanda.setTags(new java.util.LinkedHashSet<>(tags));
		demanda.setPresupuestoEstimado(new BigDecimal("100.00"));
		demanda.setEstado("ACTIVA");
		demanda.setFechaCreacion(LocalDateTime.now());
		return demanda;
	}

	private Producto producto(Long id, Usuario usuario, String titulo, String categoria, String campus) {
		Producto producto = new Producto();
		producto.setId(id);
		producto.setUsuario(usuario);
		producto.setTitulo(titulo);
		producto.setDescripcion("Modelo Casio fx-991");
		producto.setCategoria(categoria);
		producto.setCampus(campus);
		producto.setPrecio(new BigDecimal("50.00"));
		producto.setEstado("ACTIVO");
		producto.setCreatedAt(LocalDateTime.now());
		return producto;
	}

	private Usuario usuario(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNombre("Usuario " + id);
		usuario.setCorreoInstitucional("usuario" + id + "@utp.edu.pe");
		usuario.setPasswordHash("hash");
		usuario.setRol("ESTUDIANTE");
		return usuario;
	}
}
