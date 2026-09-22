package com.uMarket.uMarket.service;

import com.uMarket.uMarket.repository.UsuarioRepository;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
		return usuarioRepository.findByCorreoInstitucional(correo)
				.map(UsuarioPrincipal::new)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));
	}
}