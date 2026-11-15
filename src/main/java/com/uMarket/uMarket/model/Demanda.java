package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "demandas")
@Getter
@Setter
@NoArgsConstructor
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

	@Column(length = 80)
	private String categoria;

	/** Campus donde se espera la entrega; se usa para el cruce espacial. */
	@Column(length = 80)
	private String campus;

	/** Etiquetas normalizadas para el matching de demanda inversa. */
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "demanda_tags", joinColumns = @JoinColumn(name = "demanda_id"))
	@Column(name = "tag", length = 40, nullable = false)
	private Set<String> tags = new LinkedHashSet<>();

	@Column(nullable = false, length = 30)
	private String estado = "ACTIVA";

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@Version
	private Long version;

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
