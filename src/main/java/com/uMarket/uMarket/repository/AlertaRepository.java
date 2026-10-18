package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

	List<Alerta> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

	List<Alerta> findByUsuarioIdAndLeidaFalse(Long usuarioId);
}
