package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demandas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Demanda {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String titulo;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	@Column(name = "presupuesto_estimado", precision = 10, scale = 2)
	private BigDecimal presupuestoEstimado;

	@Column(nullable = false, length = 30)
	private String estado = "ACTIVA";

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@PrePersist
	protected void onCreate() {
		if (estado == null) {
			estado = "ACTIVA";
		}
		if (fechaCreacion == null) {
			fechaCreacion = LocalDateTime.now();
		}
	}
}
