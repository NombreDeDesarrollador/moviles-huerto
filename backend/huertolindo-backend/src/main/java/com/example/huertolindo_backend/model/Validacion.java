package com.example.huertolindo_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "validaciones")
@Getter
@Setter
@NoArgsConstructor
public class Validacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean aprobado;

    @Column(length = 1000)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fechaValidacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validador_id", nullable = false)
    private Usuario validador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id")
    private Tarea tarea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidencia_id")
    private Evidencia evidencia;
}
