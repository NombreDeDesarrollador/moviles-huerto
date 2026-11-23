package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.ValidacionRequest;
import com.example.huertolindo_backend.dto.ApiDtos.ValidacionResponse;
import com.example.huertolindo_backend.service.HuertoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validaciones")
@PreAuthorize("hasRole('ADMIN')")
public class ValidacionController {
    private final HuertoService service;

    public ValidacionController(HuertoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ValidacionResponse crear(@Valid @RequestBody ValidacionRequest request, Authentication authentication) {
        return service.crearValidacion(request, authentication.getName());
    }

    @GetMapping
    public List<ValidacionResponse> listar() {
        return service.validaciones();
    }
}
