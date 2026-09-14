package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.Raca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RacaRepository extends JpaRepository<Raca, Long> {
    List<Raca> findByEspecieOrderByNomeAsc(String especie);
}
