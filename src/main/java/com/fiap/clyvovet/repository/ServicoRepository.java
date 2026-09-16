package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findByClinicaIdAndAtivoTrue(Long clinicaId);
    List<Servico> findByAtivoTrue();
}
