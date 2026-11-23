package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.*;
import com.example.huertolindo_backend.service.HuertoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {
    private final HuertoService service;

    public TareaController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TareaResponse> listar(@RequestParam(required = false) Long usuarioId,
                                      @RequestParam(required = false) Long cultivoId,
                                      Authentication authentication) {
        return service.tareas(usuarioId, cultivoId, authentication.getName(), esAdmin(authentication));
    }

    @GetMapping("/{id}")
    public TareaResponse obtener(@PathVariable Long id, Authentication authentication) {
        return service.tarea(id, authentication.getName(), esAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TareaResponse crear(@Valid @RequestBody TareaRequest request) {
        return service.crearTarea(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TareaResponse actualizar(@PathVariable Long id, @Valid @RequestBody TareaRequest request) {
        return service.actualizarTarea(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        service.eliminarTarea(id);
    }

    @PatchMapping("/{id}/completar")
    public TareaResponse completar(@PathVariable Long id, Authentication authentication) {
        return service.completarTarea(id, authentication.getName(), esAdmin(authentication));
    }

    @PostMapping("/{id}/actividades")
    @ResponseStatus(HttpStatus.CREATED)
    public ActividadResponse agregarActividad(@PathVariable Long id,
                                              @Valid @RequestBody ActividadRequest request,
                                              Authentication authentication) {
        return service.agregarActividad(id, request, authentication.getName(), esAdmin(authentication));
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
