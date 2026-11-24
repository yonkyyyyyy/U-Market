package com.uMarket.uMarket.dto;

import java.util.List;

public record GenerarCopyResponse(List<String> copys) {

	public GenerarCopyResponse {
		copys = List.copyOf(copys);
	}
}
