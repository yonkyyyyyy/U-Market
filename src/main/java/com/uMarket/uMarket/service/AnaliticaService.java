package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.MetricasProductoResponse;
import com.uMarket.uMarket.dto.RegistrarInteraccionRequest;

public interface AnaliticaService {
	void registrar(RegistrarInteraccionRequest request);
	MetricasProductoResponse metricas(Long productoId);
	java.util.List<MetricasProductoResponse> resumen();
}
