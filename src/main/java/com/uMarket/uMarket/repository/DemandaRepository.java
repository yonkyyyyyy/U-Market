package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DemandaRepository extends JpaRepository<Demanda, Long> {

	List<Demanda> findByUsuario(Usuario usuario);

	List<Demanda> findByEstadoAndFechaCreacionBefore(String estado, LocalDateTime fechaLimite);

	Page<Demanda> findByEstado(String estado, Pageable pageable);

	Page<Demanda> findByUsuario(Usuario usuario, Pageable pageable);

}