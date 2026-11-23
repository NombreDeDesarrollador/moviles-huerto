package com.example.huertolindo_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tareas")
@Getter
@Setter
@NoArgsConstructor
public class Tarea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTarea tipo;

    @Column(nullable = false)
    private LocalDate fechaProgramada;

    @Column(nullable = false)
    private boolean completada;

    @Column
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cultivo_id", nullable = false)
    private Cultivo cultivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Usuario responsable;

    public String getEstado() {
        if (estado == null || estado.isBlank()) {
            return completada ? "COMPLETADA" : "PENDIENTE";
        }
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
        if ("COMPLETADA".equalsIgnoreCase(estado) || "VALIDADA".equalsIgnoreCase(estado)) {
            this.completada = true;
        } else if ("PENDIENTE".equalsIgnoreCase(estado) || "EN_DESARROLLO".equalsIgnoreCase(estado) || "PENDIENTE_VALIDACION".equalsIgnoreCase(estado)) {
            this.completada = false;
        }
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
        if (completada) {
            if (this.estado == null || "PENDIENTE".equalsIgnoreCase(this.estado) || "EN_DESARROLLO".equalsIgnoreCase(this.estado)) {
                this.estado = "COMPLETADA";
            }
        }
    }
}
