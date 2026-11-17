package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.DemandaRequest;
import com.uMarket.uMarket.dto.DemandaRequestDTO;
import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.DemandaRepository;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DemandaServiceImpl implements DemandaService {

	private final DemandaRepository demandaRepository;

	public DemandaServiceImpl(DemandaRepository demandaRepository) {
		this.demandaRepository = demandaRepository;
	}

	// --- API nueva ---

	@Override
	@Transactional
	public DemandaResponseDTO crear(DemandaRequestDTO request) {
		return DemandaResponseDTO.from(demandaRepository.save(nuevaDemanda(usuarioAutenticado(), request)));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<DemandaResponseDTO> listarTodas(String estado, Pageable pageable) {
		Page<Demanda> page = (estado == null || estado.isBlank())
				? demandaRepository.findAll(pageable)
				: demandaRepository.findByEstado(estado.trim().toUpperCase(), pageable);
		return page.map(DemandaResponseDTO::from);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<DemandaResponseDTO> listarMisDemandas(Pageable pageable) {
		return demandaRepository.findByUsuario(usuarioAutenticado(), pageable)
				.map(DemandaResponseDTO::from);
	}

	/**
	 * Actualización con Bloqueo Optimista transparente.
	 * <p>
	 * Flujo ACID: dentro de la transacción se carga la entidad gestionada
	 * (con su campo {@code @Version}), se mutan los campos y se invoca
	 * {@code save()}. En el flush/commit Hibernate ejecuta:
	 * {@code UPDATE demandas SET ..., version = version + 1 WHERE id = ? AND version = ?}.
	 * Si otro usuario modificó el registro entre la lectura y la escritura,
	 * la condición del WHERE falla (0 filas afectadas) y JPA lanza
	 * {@code ObjectOptimisticLockingFailureException} → HTTP 409 Conflict
	 * gestionado en {@code GlobalExceptionHandler}. No se requiere código
	 * adicional: el control de concurrencia es transparente.
	 */
	@Override
	@Transactional
	public DemandaResponseDTO actualizar(Long id, DemandaRequestDTO request) {
		Usuario usuario = usuarioAutenticado();
		Demanda demanda = obtenerEntidad(id);
		verificarPropietario(demanda, usuario);
		demanda.setTitulo(request.titulo().trim());
		demanda.setDescripcion(request.descripcion());
		demanda.setPresupuestoEstimado(request.presupuestoEstimado());
		demanda.setCategoria(normalizarCategoria(request.categoria()));
		demanda.setCampus(normalizarCampus(request.campus()));
		demanda.setTags(normalizarTags(request.tags()));
		// save() dispara el chequeo optimista: compara e incrementa version automáticamente
		return DemandaResponseDTO.from(demandaRepository.save(demanda));
	}

	// --- API existente ---

	@Override
	@Transactional(readOnly = true)
	public List<DemandaResponseDTO> listarTodas() {
		return demandaRepository.findAll().stream().map(DemandaResponseDTO::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DemandaResponseDTO> listarPorUsuario(Usuario usuario) {
		return demandaRepository.findByUsuario(usuario).stream().map(DemandaResponseDTO::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DemandaResponseDTO obtener(Long id) {
		return DemandaResponseDTO.from(obtenerEntidad(id));
	}

	@Override
	@Transactional
	public DemandaResponseDTO crear(Usuario usuario, DemandaRequest request) {
		Demanda demanda = new Demanda();
		demanda.setUsuario(usuario);
		demanda.setTitulo(request.titulo().trim());
		demanda.setDescripcion(request.descripcion());
		demanda.setPresupuestoEstimado(request.presupuestoEstimado());
		demanda.setFechaCreacion(LocalDateTime.now());
		return DemandaResponseDTO.from(demandaRepository.save(demanda));
	}

	@Override
	@Transactional
	public DemandaResponseDTO crear(Usuario usuario, DemandaRequestDTO request) {
		return DemandaResponseDTO.from(demandaRepository.save(nuevaDemanda(usuario, request)));
	}

	@Override
	@Transactional
	public DemandaResponseDTO actualizar(Long id, Usuario usuario, DemandaRequest request) {
		Demanda demanda = obtenerEntidad(id);
		verificarPropietario(demanda, usuario);
		demanda.setTitulo(request.titulo().trim());
		demanda.setDescripcion(request.descripcion());
		demanda.setPresupuestoEstimado(request.presupuestoEstimado());
		return DemandaResponseDTO.from(demandaRepository.save(demanda));
	}

	@Override
	@Transactional
	public void eliminar(Long id, Usuario usuario) {
		Demanda demanda = obtenerEntidad(id);
		verificarPropietario(demanda, usuario);
		demandaRepository.delete(demanda);
	}

	// --- Helpers ---

	private Demanda nuevaDemanda(Usuario usuario, DemandaRequestDTO request) {
		Demanda demanda = new Demanda();
		demanda.setUsuario(usuario);
		demanda.setTitulo(request.titulo().trim());
		demanda.setDescripcion(request.descripcion());
		demanda.setPresupuestoEstimado(request.presupuestoEstimado());
		demanda.setCategoria(normalizarCategoria(request.categoria()));
		demanda.setCampus(normalizarCampus(request.campus()));
		demanda.setTags(normalizarTags(request.tags()));
		demanda.setEstado("ACTIVA");
		demanda.setFechaCreacion(LocalDateTime.now());
		return demanda;
	}

	private Usuario usuarioAutenticado() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UsuarioPrincipal up) {
			return up.getUsuario();
		}
		throw new IllegalStateException("No hay usuario autenticado en el contexto de seguridad");
	}

	private String normalizarCategoria(String categoria) {
		if (categoria == null || categoria.isBlank()) {
			return null;
		}
		return categoria.trim().toUpperCase(java.util.Locale.ROOT);
	}

	private String normalizarCampus(String campus) {
		if (campus == null || campus.isBlank()) {
			return null;
		}
		return campus.trim();
	}

	/**
	 * Normaliza las etiquetas para que el matching sea consistente: sin tildes,
	 * en minúsculas, sin duplicados y con un máximo de 10.
	 */
	private Set<String> normalizarTags(List<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return new LinkedHashSet<>();
		}
		return tags.stream()
				.filter(Objects::nonNull)
				.map(this::normalizarTag)
				.filter(tag -> !tag.isBlank())
				.distinct()
				.limit(10)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private String normalizarTag(String tag) {
		String nfd = Normalizer.normalize(tag, Normalizer.Form.NFD);
		String plano = nfd.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.trim()
				.replaceAll("\\s+", " ");
		return plano.length() > 40 ? plano.substring(0, 40) : plano;
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
