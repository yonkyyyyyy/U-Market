package com.uMarket.uMarket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank(message = "El nombre es obligatorio")
		String nombre,

		@NotBlank(message = "El correo es obligatorio")
		@Email(message = "Formato de correo inválido")
		@Pattern(regexp = "^[A-Za-z0-9._%+-]+@utp\\.edu\\.pe$",
				message = "El correo debe pertenecer al dominio institucional @utp.edu.pe")
		String correo,

		@NotBlank(message = "La contraseña es obligatoria")
		@Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
		String password,

		String rol
) {
}