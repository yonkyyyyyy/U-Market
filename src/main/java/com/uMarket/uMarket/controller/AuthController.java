package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.AuthResponse;
import com.uMarket.uMarket.dto.LoginRequest;
import com.uMarket.uMarket.dto.RegisterRequest;
import com.uMarket.uMarket.dto.UserDto;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/me")
	public ResponseEntity<UserDto> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.ok(UserDto.from(principal.getUsuario()));
	}
}