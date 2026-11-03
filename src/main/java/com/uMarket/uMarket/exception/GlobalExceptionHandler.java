package com.uMarket.uMarket.exception;

import jakarta.persistence.OptimisticLockException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
		return build(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta operación");
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
		return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return build(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(ContenidoInapropiadoException.class)
	public ResponseEntity<Map<String, Object>> handleContenidoInapropiado(ContenidoInapropiadoException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("status", 422);
		body.put("message", ex.getMessage());
		body.put("categorias", ex.getResultado().getCategoriasDetectadas());
		return ResponseEntity.status(422).body(body);
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<Map<String, Object>> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
		return build(HttpStatus.CONFLICT,
				"El registro fue modificado por otro usuario y debe recargar la página");
	}

	@ExceptionHandler(OptimisticLockException.class)
	public ResponseEntity<Map<String, Object>> handleOptimisticLockException(OptimisticLockException ex) {
		return build(HttpStatus.CONFLICT,
				"El registro fue modificado por otro usuario y debe recargar la página");
	}

	private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("status", status.value());
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}