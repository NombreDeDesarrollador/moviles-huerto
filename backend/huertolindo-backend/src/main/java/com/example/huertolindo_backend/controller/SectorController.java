package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.SectorRequest;
import com.example.huertolindo_backend.dto.ApiDtos.SectorResponse;
import com.example.huertolindo_backend.service.HuertoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sectores")
public class SectorController {
    private final HuertoService service;

    public SectorController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    public List<SectorResponse> listar() {
        return service.sectores();
    }

    @GetMapping("/{id}")
    public SectorResponse obtener(@PathVariable Long id) {
        return service.sector(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SectorResponse crear(@Valid @RequestBody SectorRequest request) {
        return service.crearSector(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SectorResponse actualizar(@PathVariable Long id, @Valid @RequestBody SectorRequest request) {
        return service.actualizarSector(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        service.eliminarSector(id);
    }
}
