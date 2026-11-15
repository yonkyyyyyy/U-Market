package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProductoNlpSearchRepositoryImpl implements ProductoNlpSearchRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Producto> buscarCandidatosNlp(Specification<Producto> specification, Pageable pageable) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Producto> query = builder.createQuery(Producto.class);
		Root<Producto> producto = query.from(Producto.class);
		Predicate predicate = specification.toPredicate(producto, query, builder);

		query.select(producto).where(predicate);
		if (pageable.getSort().isSorted()) {
			query.orderBy(pageable.getSort().stream()
					.map(order -> order.isAscending()
							? builder.asc(producto.get(order.getProperty()))
							: builder.desc(producto.get(order.getProperty())))
					.toList());
		}

		return entityManager.createQuery(query)
				.setFirstResult(Math.toIntExact(pageable.getOffset()))
				.setMaxResults(pageable.getPageSize())
				.getResultList();
	}
}
