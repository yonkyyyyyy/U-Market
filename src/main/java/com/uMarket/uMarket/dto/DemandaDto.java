package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.model.Demanda;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DemandaDto(Long id, String titulo, String descripcion, BigDecimal presupuestoEstimado,
						 LocalDateTime createdAt, Long usuarioId, String usuarioNombre) {

	public static DemandaDto from(Demanda demanda) {
		return new DemandaDto(
				demanda.getId(),
				demanda.getTitulo(),
				demanda.getDescripcion(),
				demanda.getPresupuestoEstimado(),
				demanda.getCreatedAt(),
				demanda.getUsuario().getId(),
				demanda.getUsuario().getNombre());
	}
}