package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_demanda", indexes = {
		@Index(name = "idx_auditoria_demanda_id", columnList = "demanda_id"),
		@Index(name = "idx_auditoria_admin_id", columnList = "admin_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaDemanda {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "demanda_id", nullable = false)
	private Long demandaId;

	@Column(name = "admin_id", nullable = false)
	private Long adminId;

	@Column(nullable = false, length = 30)
	private String accion;

	@Column(columnDefinition = "TEXT")
	private String motivo;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@PrePersist
	protected void onCreate() {
		if (fechaCreacion == null) {
			fechaCreacion = LocalDateTime.now();
		}
	}
}
