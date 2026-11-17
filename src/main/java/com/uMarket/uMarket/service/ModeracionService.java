package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.AuditoriaDemanda;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.repository.AuditoriaDemandaRepository;
import com.uMarket.uMarket.repository.DemandaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModeracionService {

	public static final String ESTADO_BLOQUEADA = "BLOQUEADA";
	public static final String ACCION_BLOQUEO = "BLOQUEO";

	private final DemandaRepository demandaRepository;
	private final AuditoriaDemandaRepository auditoriaRepository;

	public ModeracionService(DemandaRepository demandaRepository,
			AuditoriaDemandaRepository auditoriaRepository) {
		this.demandaRepository = demandaRepository;
		this.auditoriaRepository = auditoriaRepository;
	}

	/**
	 * Lista TODAS las demandas para el panel de moderación, sin filtrar por
	 * estado (incluye ACTIVA, CUMPLIDA, CANCELADA y BLOQUEADA).
	 */
	@Transactional(readOnly = true)
	public Page<DemandaResponseDTO> listarTodas(Pageable pageable) {
		return demandaRepository.findAll(pageable).map(DemandaResponseDTO::from);
	}

	/**
	 * Bloquea una demanda y registra la acción en auditoría dentro de la misma
	 * transacción: si falla el guardado de la auditoría, se revierte el cambio
	 * de estado. Incluye control optimista: el {@code save()} verifica la
	 * columna {@code version} de forma transparente.
	 * SQL generado: UPDATE demandas SET estado=?, version=version+1 WHERE id=? AND version=?
	 * Si dos moderadores intentan bloquear la misma demanda concurrentemente,
	 * solo uno tiene éxito; el otro recibe 409 Conflict.
	 *
	 * @param demandaId id de la demanda a bloquear
	 * @param adminId   id del administrador que ejecuta la acción (viene del JWT)
	 * @param motivo    motivo del bloqueo (obligatorio)
	 */
	@Transactional
	public DemandaResponseDTO bloquear(Long demandaId, Long adminId, String motivo) {
		if (motivo == null || motivo.isBlank()) {
			throw new IllegalArgumentException("El motivo del bloqueo es obligatorio");
		}
		Demanda demanda = demandaRepository.findById(demandaId)
				.orElseThrow(() -> new ResourceNotFoundException("Demanda no encontrada con id: " + demandaId));

		if (ESTADO_BLOQUEADA.equals(demanda.getEstado())) {
			throw new IllegalArgumentException("La demanda ya se encuentra BLOQUEADA");
		}

		demanda.setEstado(ESTADO_BLOQUEADA);
		// Chequeo optimista transparente: si version colisiona lanza ObjectOptimisticLockingFailureException
		Demanda guardada = demandaRepository.save(demanda);

		AuditoriaDemanda auditoria = new AuditoriaDemanda();
		auditoria.setDemandaId(guardada.getId());
		auditoria.setAdminId(adminId);
		auditoria.setAccion(ACCION_BLOQUEO);
		auditoria.setMotivo(motivo.trim());
		auditoria.setFechaCreacion(LocalDateTime.now());
		auditoriaRepository.save(auditoria);

		return DemandaResponseDTO.from(guardada);
	}

	@Transactional(readOnly = true)
	public List<AuditoriaDemanda> historial(Long demandaId) {
		if (!demandaRepository.existsById(demandaId)) {
			throw new ResourceNotFoundException("Demanda no encontrada con id: " + demandaId);
		}
		return auditoriaRepository.findByDemandaIdOrderByFechaCreacionDesc(demandaId);
	}
}
