package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.DemandaRequest;
import com.uMarket.uMarket.dto.DemandaRequestDTO;
import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DemandaService {

	// --- API nueva (usa SecurityContext / JWT internamente) ---
	DemandaResponseDTO crear(DemandaRequestDTO request);

	Page<DemandaResponseDTO> listarTodas(String estado, Pageable pageable);

	Page<DemandaResponseDTO> listarMisDemandas(Pageable pageable);

	DemandaResponseDTO actualizar(Long id, DemandaRequestDTO request);

	// --- API existente (compatibilidad con código y tests actuales) ---
	List<DemandaResponseDTO> listarTodas();

	List<DemandaResponseDTO> listarPorUsuario(Usuario usuario);

	DemandaResponseDTO obtener(Long id);

	DemandaResponseDTO crear(Usuario usuario, DemandaRequest request);

	DemandaResponseDTO crear(Usuario usuario, DemandaRequestDTO request);

	DemandaResponseDTO actualizar(Long id, Usuario usuario, DemandaRequest request);

	void eliminar(Long id, Usuario usuario);
}
