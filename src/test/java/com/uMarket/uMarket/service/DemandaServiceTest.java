package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.DemandaDto;
import com.uMarket.uMarket.dto.DemandaRequest;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.DemandaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandaServiceTest {

	@Mock
	private DemandaRepository demandaRepository;

	private DemandaService demandaService;

	@BeforeEach
	void setUp() {
		demandaService = new DemandaService(demandaRepository);
	}

	@Test
	void listarTodasDevuelveListaMapeada() {
		when(demandaRepository.findAll()).thenReturn(List.of(demanda(1L, usuario(1L))));

		List<DemandaDto> result = demandaService.listarTodas();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).titulo()).isEqualTo("Busco libro de cálculo");
	}

	@Test
	void crearAsignaUsuarioYTitulo() {
		Usuario autor = usuario(1L);
		when(demandaRepository.save(any(Demanda.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DemandaRequest request = new DemandaRequest("Busco libro de cálculo", "Cálculo de una variable", new BigDecimal("60.00"));

		DemandaDto result = demandaService.crear(autor, request);

		assertThat(result.titulo()).isEqualTo("Busco libro de cálculo");
		assertThat(result.usuarioId()).isEqualTo(1L);
		assertThat(result.presupuestoEstimado()).isEqualByComparingTo("60.00");
	}

	@Test
	void obtenerLanza404SiNoExiste() {
		when(demandaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> demandaService.obtener(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void actualizarBloqueaAUnNoPropietario() {
		Demanda demanda = demanda(1L, usuario(1L));
		when(demandaRepository.findById(1L)).thenReturn(Optional.of(demanda));

		DemandaRequest request = new DemandaRequest("Título", "Desc", new BigDecimal("10.00"));

		assertThatThrownBy(() -> demandaService.actualizar(1L, usuario(2L), request))
				.isInstanceOf(AccessDeniedException.class);

		verify(demandaRepository, never()).save(any());
	}

	@Test
	void eliminarBloqueaAUnNoPropietario() {
		Demanda demanda = demanda(1L, usuario(1L));
		when(demandaRepository.findById(1L)).thenReturn(Optional.of(demanda));

		assertThatThrownBy(() -> demandaService.eliminar(1L, usuario(2L)))
				.isInstanceOf(AccessDeniedException.class);

		verify(demandaRepository, never()).delete(any());
	}

	private Usuario usuario(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNombre("Ana Torres");
		usuario.setCorreoInstitucional("ana@utp.edu.pe");
		usuario.setPasswordHash("hash");
		usuario.setRol("ESTUDIANTE");
		return usuario;
	}

	private Demanda demanda(Long id, Usuario usuario) {
		Demanda demanda = new Demanda();
		demanda.setId(id);
		demanda.setUsuario(usuario);
		demanda.setTitulo("Busco libro de cálculo");
		demanda.setDescripcion("Cálculo de una variable");
		demanda.setPresupuestoEstimado(new BigDecimal("60.00"));
		return demanda;
	}
}