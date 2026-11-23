package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.*;
import com.example.huertolindo_backend.service.HuertoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cultivos")
public class CultivoController {
    private final HuertoService service;

    public CultivoController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    public List<CultivoResponse> listar(@RequestParam(required = false) Long sectorId) {
        return service.cultivos(sectorId);
    }

    @GetMapping("/{id}")
    public CultivoResponse obtener(@PathVariable Long id) {
        return service.cultivo(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CultivoResponse crear(@Valid @RequestBody CultivoRequest request) {
        return service.crearCultivo(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CultivoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CultivoRequest request) {
        return service.actualizarCultivo(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public CultivoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoCultivoRequest request) {
        return service.cambiarEstadoCultivo(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        service.eliminarCultivo(id);
    }
}
