package com.example.huertolindo_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "cultivos")
@Getter
@Setter
@NoArgsConstructor
public class Cultivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String especie;

    private LocalDate fechaSiembra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCultivo estado = EstadoCultivo.SEMBRADO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;
}
