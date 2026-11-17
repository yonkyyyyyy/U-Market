package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.model.Demanda;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DemandaResponseDTO(
		Long id,
		String titulo,
		String descripcion,
		BigDecimal presupuestoEstimado,
		String categoria,
		String estado,
		LocalDateTime fechaCreacion,
		Long usuarioId,
		String usuarioNombre
) {

	public static DemandaResponseDTO from(Demanda demanda) {
		return new DemandaResponseDTO(
				demanda.getId(),
				demanda.getTitulo(),
				demanda.getDescripcion(),
				demanda.getPresupuestoEstimado(),
				demanda.getCategoria(),
				demanda.getEstado(),
				demanda.getFechaCreacion(),
				demanda.getUsuario().getId(),
				demanda.getUsuario().getNombre());
	}
}
