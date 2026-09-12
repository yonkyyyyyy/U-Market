package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.AuthResponse;
import com.uMarket.uMarket.dto.LoginRequest;
import com.uMarket.uMarket.dto.RegisterRequest;
import com.uMarket.uMarket.dto.UserDto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.UsuarioRepository;
import com.uMarket.uMarket.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

	private static final Pattern UTP_EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@utp\\.edu\\.pe$", Pattern.CASE_INSENSITIVE);

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
					   JwtService jwtService, AuthenticationManager authenticationManager) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String correo = request.correo().trim().toLowerCase(Locale.ROOT);
		if (!UTP_EMAIL.matcher(correo).matches()) {
			throw new IllegalArgumentException("El correo debe pertenecer al dominio institucional @utp.edu.pe");
		}
		if (usuarioRepository.existsByCorreoInstitucional(correo)) {
			throw new IllegalArgumentException("El correo ya está registrado");
		}

		Usuario usuario = new Usuario();
		usuario.setNombre(request.nombre().trim());
		usuario.setCorreoInstitucional(correo);
		usuario.setPasswordHash(passwordEncoder.encode(request.password()));
		usuario.setRol(request.rol() == null || request.rol().isBlank()
				? "ESTUDIANTE"
				: request.rol().trim().toUpperCase(Locale.ROOT));
		usuario.setCreatedAt(LocalDateTime.now());
		usuarioRepository.save(usuario);

		return new AuthResponse(jwtService.generateToken(usuario), UserDto.from(usuario));
	}

	public AuthResponse login(LoginRequest request) {
		String correo = request.correo().trim().toLowerCase(Locale.ROOT);
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, request.password()));
		Usuario usuario = usuarioRepository.findByCorreoInstitucional(correo)
				.orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

		return new AuthResponse(jwtService.generateToken(usuario), UserDto.from(usuario));
	}
}