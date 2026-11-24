package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.enums.TipoEvento;
import jakarta.validation.constraints.NotNull;

public record RegistrarInteraccionRequest(
		@NotNull Long productoId,
		Long usuarioId,
		@NotNull TipoEvento tipoEvento
) {}
