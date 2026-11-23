package com.example.huertolindo_backend.repository;

import com.example.huertolindo_backend.model.Cultivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CultivoRepository extends JpaRepository<Cultivo, Long> {
    List<Cultivo> findBySectorId(Long sectorId);
}
