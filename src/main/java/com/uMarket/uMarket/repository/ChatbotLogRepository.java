package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.ChatbotLog;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotLogRepository extends JpaRepository<ChatbotLog, Long> {

	List<ChatbotLog> findByUsuario(Usuario usuario);

}