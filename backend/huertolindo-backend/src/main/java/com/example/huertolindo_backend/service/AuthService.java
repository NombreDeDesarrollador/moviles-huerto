package com.example.huertolindo_backend.service;

import com.example.huertolindo_backend.dto.ApiDtos.*;
import com.example.huertolindo_backend.exception.NotFoundException;
import com.example.huertolindo_backend.model.Rol;
import com.example.huertolindo_backend.model.Usuario;
import com.example.huertolindo_backend.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public Authentication login(LoginRequest request) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password()));
    }

    @Transactional
    public UsuarioResponse register(RegistroRequest request) {
        if (usuarios.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.ESTUDIANTE);
        usuario.setActivo(true);
        return toResponse(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse currentUser(String correo) {
        return usuarios.findByCorreo(correo).map(AuthService::toResponse)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), usuario.isActivo());
    }
}
