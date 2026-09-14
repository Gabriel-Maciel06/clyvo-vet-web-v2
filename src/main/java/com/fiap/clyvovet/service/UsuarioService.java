package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CadastroUsuarioDto;
import com.fiap.clyvovet.model.ProviderAutenticacao;
import com.fiap.clyvovet.model.RoleUsuario;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                           TutorRepository tutorRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Usuario buscarOuFalhar(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));
    }

    /**
     * Autocadastro público do tutor: cria a conta de acesso (T_USUARIO, sempre
     * com ROLE_TUTOR — administradores/veterinários não se autocadastram) e o
     * registro de Tutor (T_TUTOR) vinculado a ela, na mesma transação.
     */
    @Transactional
    public Usuario cadastrarTutor(CadastroUsuarioDto dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Este nome de usuário já está em uso.");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Já existe uma conta cadastrada com este e-mail.");
        }
        String cpf = apenasDigitos(dto.getCpf());
        if (tutorRepository.existsById(cpf)) {
            throw new IllegalArgumentException("Já existe um tutor cadastrado com este CPF.");
        }

        Usuario usuario = cadastrarUsuario(dto.getNomeCompleto(), dto.getEmail(), dto.getUsername(),
                dto.getSenha(), RoleUsuario.ROLE_TUTOR);

        Tutor tutor = new Tutor();
        tutor.setCpf(cpf);
        tutor.setNome(dto.getNomeCompleto());
        tutor.setTelefone(dto.getTelefone());
        tutor.setEmail(dto.getEmail());
        tutor.setUsuario(usuario);
        tutorRepository.save(tutor);

        return usuario;
    }

    /** Cadastro de baixo nível, usado tanto pelo autocadastro quanto pelo login social. */
    @Transactional
    public Usuario cadastrarUsuario(String nomeCompleto, String email, String username, String senhaPura, RoleUsuario role) {
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(nomeCompleto);
        usuario.setEmail(email);
        usuario.setUsername(username);
        usuario.setPassword(senhaPura != null ? passwordEncoder.encode(senhaPura) : null);
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    private String apenasDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }
}
