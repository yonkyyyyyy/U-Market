package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findByUsuario(Usuario usuario);

	List<Producto> findByEstado(String estado);

}