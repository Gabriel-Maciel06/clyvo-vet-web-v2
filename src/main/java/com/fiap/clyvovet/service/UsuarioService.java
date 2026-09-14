package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.RoleUsuario;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Usuario buscarOuFalhar(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));
    }

    @Transactional
    public Usuario cadastrarUsuario(String nomeCompleto, String email, String username, String senhaPura, RoleUsuario role) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Nome de usuário já em uso: " + username);
        }
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(nomeCompleto);
        usuario.setEmail(email);
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(senhaPura));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }
}
