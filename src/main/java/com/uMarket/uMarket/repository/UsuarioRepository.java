package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

}