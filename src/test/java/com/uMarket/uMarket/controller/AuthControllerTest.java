package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.AuthResponse;
import com.uMarket.uMarket.dto.LoginRequest;
import com.uMarket.uMarket.dto.RegisterRequest;
import com.uMarket.uMarket.dto.UserDto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.security.JwtService;
import com.uMarket.uMarket.service.AuthService;
import com.uMarket.uMarket.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	void registrarCorreoUtpDevuelve201() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
				.thenReturn(new AuthResponse("tokenJwt", new UserDto(1L, "Ana Torres", "ana@utp.edu.pe", "ESTUDIANTE")));

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Ana Torres\",\"correo\":\"ana@utp.edu.pe\",\"password\":\"password123\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").value("tokenJwt"))
				.andExpect(jsonPath("$.user.correo").value("ana@utp.edu.pe"));
	}

	@Test
	void registrarCorreoFueraDelDominioDa400() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Ana\",\"correo\":\"ana@gmail.com\",\"password\":\"password123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("@utp.edu.pe")));

		verify(authService, never()).register(any());
	}

	@Test
	void loginDevuelve200YTutoken() throws Exception {
		when(authService.login(any(LoginRequest.class)))
				.thenReturn(new AuthResponse("tokenJwt", new UserDto(1L, "Ana Torres", "ana@utp.edu.pe", "ESTUDIANTE")));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"ana@utp.edu.pe\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("tokenJwt"));
	}

	@Test
	void loginConDatosVaciosDa400() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"\",\"password\":\"\"}"))
				.andExpect(status().isBadRequest());

		verify(authService, never()).login(any());
	}

	@Test
	void meDevuelveUsuarioAutenticado() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNombre("Ana Torres");
		usuario.setCorreoInstitucional("ana@utp.edu.pe");
		usuario.setRol("ESTUDIANTE");
		UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		SecurityContextHolder.setContext(context);

		try {
			mockMvc.perform(get("/api/auth/me"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.correo").value("ana@utp.edu.pe"));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}