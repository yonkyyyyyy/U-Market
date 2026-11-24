package com.uMarket.uMarket.dto;

import java.util.List;

public record GeneradorCopysChatResponse(List<Choice> choices) {

	public record Choice(Message message) {
	}

	public record Message(String content) {
	}

	public String contenido() {
		if (choices == null || choices.isEmpty()
				|| choices.get(0) == null || choices.get(0).message() == null) {
			return null;
		}
		return choices.get(0).message().content();
	}
}
