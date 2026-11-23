package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.UsuarioRequest;
import com.example.huertolindo_backend.dto.ApiDtos.UsuarioResponse;
import com.example.huertolindo_backend.model.Rol;
import com.example.huertolindo_backend.service.HuertoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    private final HuertoService service;

    public UsuarioController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Rol rol) {
        return service.usuarios(rol);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return service.usuario(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return service.crearUsuario(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return service.actualizarUsuario(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminarUsuario(id);
    }
}
