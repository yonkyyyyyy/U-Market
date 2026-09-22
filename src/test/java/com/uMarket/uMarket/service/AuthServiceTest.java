package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.AuthResponse;
import com.uMarket.uMarket.dto.RegisterRequest;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.UsuarioRepository;
import com.uMarket.uMarket.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtService jwtService;
	@Mock
	private AuthenticationManager authenticationManager;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(usuarioRepository, passwordEncoder, jwtService, authenticationManager);
	}

	@Test
	void registerValidaCorreoUtpYiGuardaUsuario() {
		when(usuarioRepository.existsByCorreoInstitucional("ana@utp.edu.pe")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("hashEncriptado");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtService.generateToken(any(Usuario.class))).thenReturn("tokenJwt");

		RegisterRequest request = new RegisterRequest("Ana Torres", "Ana@UTP.EDU.PE", "password123", null);

		AuthResponse response = authService.register(request);

		assertThat(response.token()).isEqualTo("tokenJwt");
		assertThat(response.user().correo()).isEqualTo("ana@utp.edu.pe");
		assertThat(response.user().rol()).isEqualTo("ESTUDIANTE");

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(captor.capture());
		assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashEncriptado");
		assertThat(captor.getValue().getRol()).isEqualTo("ESTUDIANTE");
		assertThat(captor.getValue().getCorreoInstitucional()).isEqualTo("ana@utp.edu.pe");
	}

	@Test
	void registerRechazaCorreoFueraDelDominioUtp() {
		RegisterRequest request = new RegisterRequest("Ana", "ana@gmail.com", "password123", null);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("@utp.edu.pe");

		verifyNoInteractions(usuarioRepository, passwordEncoder, jwtService);
	}

	@Test
	void registerRechazaCorreoDuplicado() {
		when(usuarioRepository.existsByCorreoInstitucional("ana@utp.edu.pe")).thenReturn(true);
		RegisterRequest request = new RegisterRequest("Ana", "ana@utp.edu.pe", "password123", null);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("ya está registrado");

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void loginExitosoDevuelveToken() {
		Usuario usuario = usuario();
		when(usuarioRepository.findByCorreoInstitucional("ana@utp.edu.pe")).thenReturn(Optional.of(usuario));
		when(jwtService.generateToken(usuario)).thenReturn("tokenJwt");

		AuthResponse response = authService.login(new com.uMarket.uMarket.dto.LoginRequest("ana@utp.edu.pe", "password123"));

		assertThat(response.token()).isEqualTo("tokenJwt");
		assertThat(response.user().correo()).isEqualTo("ana@utp.edu.pe");

		verify(authenticationManager).authenticate(any());
	}

	@Test
	void loginConCredencialesInvalidasLanzaError() {
		doThrow(new BadCredentialsException("bad credentials"))
				.when(authenticationManager).authenticate(any());

		assertThatThrownBy(() -> authService.login(new com.uMarket.uMarket.dto.LoginRequest("ana@utp.edu.pe", "incorrecta")))
				.isInstanceOf(BadCredentialsException.class);

		verify(usuarioRepository, never()).findByCorreoInstitucional(any());
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNombre("Ana Torres");
		usuario.setCorreoInstitucional("ana@utp.edu.pe");
		usuario.setPasswordHash("hashEncriptado");
		usuario.setRol("ESTUDIANTE");
		return usuario;
	}
}