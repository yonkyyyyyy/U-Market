package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandaRepository extends JpaRepository<Demanda, Long> {

	List<Demanda> findByUsuario(Usuario usuario);

}