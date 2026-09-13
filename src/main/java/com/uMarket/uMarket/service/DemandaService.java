package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.DemandaDto;
import com.uMarket.uMarket.dto.DemandaRequest;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.DemandaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemandaService {

	private final DemandaRepository demandaRepository;

	public DemandaService(DemandaRepository demandaRepository) {
		this.demandaRepository = demandaRepository;
	}

	@Transactional(readOnly = true)
	public List<DemandaDto> listarTodas() {
		return demandaRepository.findAll().stream().map(DemandaDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<DemandaDto> listarPorUsuario(Usuario usuario) {
		return demandaRepository.findByUsuario(usuario).stream().map(DemandaDto::from).toList();
	}

	@Transactional(readOnly = true)
	public DemandaDto obtener(Long id) {
		return DemandaDto.from(obtenerEntidad(id));
	}

	@Transactional
	public DemandaDto crear(Usuario usuario, DemandaRequest request) {
		Demanda demanda = new Demanda();
		demanda.setUsuario(usuario);
		demanda.setTitulo(request.titulo().trim());
		demanda.setDescripcion(request.descripcion());
		demanda.setPresupuestoEstimado(request.presupuestoEstimado());
		demanda.setCreatedAt(LocalDateTime.now());
		return DemandaDto.from(demandaRepository.save(demanda));
	}

	@Transactional
	public DemandaDto actualizar(Long id, Usuario usuario, DemandaRequest request) {
		Demanda demanda = obtenerEntidad(id);
		verificarPropietario(demanda, usuario);
		demanda.setTitulo(request.titulo().trim());
		demanda.setDescripcion(request.descripcion());
		demanda.setPresupuestoEstimado(request.presupuestoEstimado());
		return DemandaDto.from(demandaRepository.save(demanda));
	}

	@Transactional
	public void eliminar(Long id, Usuario usuario) {
		Demanda demanda = obtenerEntidad(id);
		verificarPropietario(demanda, usuario);
		demandaRepository.delete(demanda);
	}

	private Demanda obtenerEntidad(Long id) {
		return demandaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Demanda no encontrada con id: " + id));
	}

	private void verificarPropietario(Demanda demanda, Usuario usuario) {
		if (!demanda.getUsuario().getId().equals(usuario.getId())) {
			throw new AccessDeniedException("No puedes modificar una demanda que no te pertenece");
		}
	}
}