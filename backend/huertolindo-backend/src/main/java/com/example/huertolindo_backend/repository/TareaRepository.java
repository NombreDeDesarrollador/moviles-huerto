package com.example.huertolindo_backend.repository;

import com.example.huertolindo_backend.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findByResponsableId(Long usuarioId);
    List<Tarea> findByCultivoId(Long cultivoId);
}
