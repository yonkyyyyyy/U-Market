package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.MatrizClasificacion;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import com.uMarket.uMarket.model.RegistroModeracion;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.security.JwtService;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.CustomUserDetailsService;
import com.uMarket.uMarket.service.RegistroModeracionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeracionIaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ModeracionIaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegistroModeracionService registroModeracionService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	private final UsuarioPrincipal principal = new UsuarioPrincipal(usuario());

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	private void autenticar() {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		SecurityContextHolder.setContext(context);
	}

	@Test
	void listarLogsDevuelveLaPaginaYPasaLosFiltros() throws Exception {
		when(registroModeracionService.listar(eq(TipoModeracion.IMAGEN), eq(VeredictoModeracion.RECHAZADO), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(registro())));

		mockMvc.perform(get("/api/admin/moderacion/ia/logs")
						.param("tipo", "IMAGEN")
						.param("veredicto", "RECHAZADO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].veredicto").value("RECHAZADO"))
				.andExpect(jsonPath("$.content[0].productoTitulo").value("Pendrive"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void obtenerMetricasDevuelveLaMatriz() throws Exception {
		when(registroModeracionService.calcularMatriz(null, null))
				.thenReturn(MatrizClasificacion.de(2, 1, 1, 3, 0, 0, 7));

		mockMvc.perform(get("/api/admin/moderacion/ia/metricas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.verdaderoPositivo").value(2))
				.andExpect(jsonPath("$.falsoPositivo").value(1))
				.andExpect(jsonPath("$.falsoNegativo").value(1))
				.andExpect(jsonPath("$.verdaderoNegativo").value(3))
				.andExpect(jsonPath("$.total").value(7));
	}

	@Test
	void revisarLogGuardaElFeedbackDelModerador() throws Exception {
		autenticar();
		RegistroModeracion revisado = registro();
		revisado.setAciertoHumano(true);
		when(registroModeracionService.revisar(eq(1L), eq(true), any(), eq(1L))).thenReturn(revisado);

		mockMvc.perform(patch("/api/admin/moderacion/ia/logs/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"aciertoHumano\":true,\"comentario\":\"correcto\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aciertoHumano").value(true));

		verify(registroModeracionService).revisar(1L, true, "correcto", 1L);
	}

	@Test
	void revisarLogSinAciertoDa400() throws Exception {
		autenticar();
		mockMvc.perform(patch("/api/admin/moderacion/ia/logs/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"comentario\":\"falta el veredicto\"}"))
				.andExpect(status().isBadRequest());
	}

	private RegistroModeracion registro() {
		RegistroModeracion registro = new RegistroModeracion();
		registro.setId(1L);
		registro.setTipo(TipoModeracion.IMAGEN);
		registro.setVeredicto(VeredictoModeracion.RECHAZADO);
		registro.setProductoId(5L);
		registro.setProductoTitulo("Pendrive");
		registro.setUsuarioId(1L);
		registro.setMotivo("Contenido prohibido detectado en la imagen");
		return registro;
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNombre("Admin UTP");
		usuario.setCorreoInstitucional("admin@utp.edu.pe");
		usuario.setPasswordHash("hash");
		usuario.setRol("ADMINISTRADOR");
		return usuario;
	}
}
