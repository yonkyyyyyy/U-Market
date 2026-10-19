package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.AuditoriaDemanda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaDemandaRepository extends JpaRepository<AuditoriaDemanda, Long> {

	List<AuditoriaDemanda> findByDemandaIdOrderByFechaCreacionDesc(Long demandaId);

	Page<AuditoriaDemanda> findByDemandaIdOrderByFechaCreacionDesc(Long demandaId, Pageable pageable);
}
