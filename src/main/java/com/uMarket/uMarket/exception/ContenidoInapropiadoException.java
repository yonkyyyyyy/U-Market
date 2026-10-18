package com.uMarket.uMarket.exception;

import com.uMarket.uMarket.dto.ModerationResultDTO;

/**
 * Se lanza cuando el contenido a publicar contiene lenguaje inapropiado.
 * El {@code GlobalExceptionHandler} la traduce a HTTP 422 (Unprocessable Entity).
 */
public class ContenidoInapropiadoException extends RuntimeException {

	private final ModerationResultDTO resultado;

	public ContenidoInapropiadoException(ModerationResultDTO resultado) {
		super("El contenido contiene lenguaje inapropiado y no puede publicarse");
		this.resultado = resultado;
	}

	public ModerationResultDTO getResultado() {
		return resultado;
	}
}