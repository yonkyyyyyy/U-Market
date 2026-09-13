package com.uMarket.uMarket.dto;

public record ImagenResult(String imagenUrl, String thumbUrl, String mediumUrl, String fullUrl) {

	public static ImagenResult from(ProcessedImage processed) {
		return new ImagenResult(
				processed.mediumUrl(),
				processed.thumbUrl(),
				processed.mediumUrl(),
				processed.fullUrl());
	}
}