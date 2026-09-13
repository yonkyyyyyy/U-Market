package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Mensaje;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

	List<Mensaje> findByRemitente(Usuario remitente);

	List<Mensaje> findByDestinatario(Usuario destinatario);

}