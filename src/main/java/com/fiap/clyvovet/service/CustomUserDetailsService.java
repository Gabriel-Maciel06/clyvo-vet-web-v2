package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        if (usuario.getPassword() == null) {
            // Conta provisionada via login social: não tem senha local para comparar.
            // Trata como "não encontrado" para o formulário devolver a mensagem
            // genérica de credenciais inválidas, em vez de quebrar com NullPointerException.
            throw new UsernameNotFoundException(
                    "Esta conta usa login com Google e não possui senha local: " + username);
        }

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                true,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(usuario.getRole().name()))
        );
    }
}
