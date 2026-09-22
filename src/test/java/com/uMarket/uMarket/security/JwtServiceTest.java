package com.uMarket.uMarket.security;

import com.uMarket.uMarket.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

	private static final String SECRET = "25d7jmEN6JOU7Jb7wsjb8YkUCdnrZFbM/L6ieishtXOGLXlHGcvrJqgGy+xyoxO1";

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(SECRET, 86400000L);
	}

	@Test
	void generaTokenConCorreoYRollDeUsuario() {
		String token = jwtService.generateToken(usuario());

		assertThat(jwtService.extractSubject(token)).isEqualTo("ana@utp.edu.pe");
		assertThat(jwtService.isValid(token, "ana@utp.edu.pe")).isTrue();
	}

	@Test
	void tokenEsInvalidoParaOtroCorreo() {
		String token = jwtService.generateToken(usuario());

		assertThat(jwtService.isValid(token, "otro@utp.edu.pe")).isFalse();
	}

	@Test
	void tokenManipuladoEsInvalido() {
		String token = jwtService.generateToken(usuario()) + "tampered";

		assertThat(jwtService.isValid(token, "ana@utp.edu.pe")).isFalse();
	}

	@Test
	void tokenExpiradoEsInvalido() {
		jwtService = new JwtService(SECRET, -1000L);
		String token = jwtService.generateToken(usuario());

		assertThat(jwtService.isValid(token, "ana@utp.edu.pe")).isFalse();
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNombre("Ana Torres");
		usuario.setCorreoInstitucional("ana@utp.edu.pe");
		usuario.setPasswordHash("hash");
		usuario.setRol("ESTUDIANTE");
		return usuario;
	}
}