package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.ReporteRequest;
import com.example.huertolindo_backend.dto.ApiDtos.ReporteResponse;
import com.example.huertolindo_backend.service.HuertoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final HuertoService service;

    public ReporteController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReporteResponse> listar() {
        return service.reportes();
    }

    @GetMapping("/{id}")
    public ReporteResponse obtener(@PathVariable Long id) {
        return service.reporte(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteResponse crear(@Valid @RequestBody ReporteRequest request, Authentication authentication) {
        return service.crearReporte(request, authentication.getName(), esAdmin(authentication));
    }

    @PutMapping("/{id}")
    public ReporteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ReporteRequest request,
                                      Authentication authentication) {
        return service.actualizarReporte(id, request, authentication.getName(), esAdmin(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        service.eliminarReporte(id);
    }

    @PatchMapping("/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteResponse resolver(@PathVariable Long id) {
        return service.resolverReporte(id);
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
