package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.config.IaInternaSecurityConfig;
import com.uMarket.uMarket.dto.VeredictoIaResponse;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import com.uMarket.uMarket.security.InternalApiKeyFilter;
import com.uMarket.uMarket.security.JwtService;
import com.uMarket.uMarket.service.CustomUserDetailsService;
import com.uMarket.uMarket.service.InferenciaIaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del controller de la API interna de IA.
 *
 * <p>A diferencia del panel de moderación, aquí los filtros de seguridad SÍ corren
 * (no se usa {@code addFilters = false}): si se desactivaran, el 401 nunca se
 * probaría y se estaría diciendo "seguridad verificada" sin verificar nada.</p>
 */
@WebMvcTest(InferenciaIaController.class)
@Import(IaInternaSecurityConfig.class)
@TestPropertySource(properties = {
		"ia.internal.api-key=clave-test",
		// Límite chico para probar el 413 sin alocar megabytes.
		"ia.limites.imagen-max-bytes=16"
})
class InferenciaIaControllerTest {

	private static final String CLAVE = "clave-test";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InferenciaIaService inferenciaIaService;

	// El slice de @WebMvcTest incluye los filtros (@Component), así que JwtAuthFilter
	// se crea y necesita sus dependencias.
	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	void sinApiKeyResponde401() throws Exception {
		mockMvc.perform(post("/api/ia/v1/texto")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"texto\":\"hola\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void conApiKeyIncorrectaResponde401() throws Exception {
		mockMvc.perform(post("/api/ia/v1/texto")
						.header(InternalApiKeyFilter.HEADER, "clave-mala")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"texto\":\"hola\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void textoValidoDevuelveElVeredicto() throws Exception {
		when(inferenciaIaService.inferirTexto("hola")).thenReturn(veredicto(TipoModeracion.TEXTO));

		mockMvc.perform(post("/api/ia/v1/texto")
						.header(InternalApiKeyFilter.HEADER, CLAVE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"texto\":\"hola\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.veredicto").value("APROBADO"))
				.andExpect(jsonPath("$.permitido").value(true))
				.andExpect(jsonPath("$.tipo").value("TEXTO"))
				.andExpect(jsonPath("$.latenciaMs").value(12));
	}

	@Test
	void textoVacioResponde400() throws Exception {
		mockMvc.perform(post("/api/ia/v1/texto")
						.header(InternalApiKeyFilter.HEADER, CLAVE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"texto\":\"   \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void imagenVaciaResponde400() throws Exception {
		mockMvc.perform(multipart("/api/ia/v1/imagen")
						.file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]))
						.header(InternalApiKeyFilter.HEADER, CLAVE))
				.andExpect(status().isBadRequest());
	}

	@Test
	void imagenDemasiadoGrandeResponde413() throws Exception {
		mockMvc.perform(multipart("/api/ia/v1/imagen")
						.file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[32]))
						.header(InternalApiKeyFilter.HEADER, CLAVE))
				.andExpect(status().isPayloadTooLarge());
	}

	@Test
	void imagenDeTipoNoSoportadoResponde415() throws Exception {
		mockMvc.perform(multipart("/api/ia/v1/imagen")
						.file(new MockMultipartFile("archivo", "doc.pdf", "application/pdf", new byte[4]))
						.header(InternalApiKeyFilter.HEADER, CLAVE))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void imagenValidaDevuelveElVeredicto() throws Exception {
		when(inferenciaIaService.inferirImagen(any(File.class))).thenReturn(veredicto(TipoModeracion.IMAGEN));

		mockMvc.perform(multipart("/api/ia/v1/imagen")
						.file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}))
						.header(InternalApiKeyFilter.HEADER, CLAVE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("IMAGEN"))
				.andExpect(jsonPath("$.permitido").value(true));
	}

	private VeredictoIaResponse veredicto(TipoModeracion tipo) {
		return new VeredictoIaResponse(VeredictoModeracion.APROBADO, true, 0.01, List.of(),
				"omni-moderation-latest", tipo, 12, LocalDateTime.now());
	}
}
