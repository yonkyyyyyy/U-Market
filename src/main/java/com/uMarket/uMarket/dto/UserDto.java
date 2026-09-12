package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.model.Usuario;

public record UserDto(Long id, String nombre, String correo, String rol) {

	public static UserDto from(Usuario usuario) {
		return new UserDto(usuario.getId(), usuario.getNombre(), usuario.getCorreoInstitucional(), usuario.getRol());
	}
}