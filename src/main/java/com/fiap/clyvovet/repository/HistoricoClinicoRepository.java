package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.HistoricoClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoClinicoRepository extends JpaRepository<HistoricoClinico, Long> {
    List<HistoricoClinico> findByPetIdOrderByDataRegistroDesc(Long petId);
}
