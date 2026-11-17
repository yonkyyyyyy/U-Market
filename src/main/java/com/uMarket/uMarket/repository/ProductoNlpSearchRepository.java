package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Producto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ProductoNlpSearchRepository {

	List<Producto> buscarCandidatosNlp(Specification<Producto> specification, Pageable pageable);
}
