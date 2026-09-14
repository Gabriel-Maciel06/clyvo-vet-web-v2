package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.ConsultaTriagem;
import com.fiap.clyvovet.model.StatusConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultaTriagemRepository extends JpaRepository<ConsultaTriagem, Long> {
    List<ConsultaTriagem> findByPetIdOrderByDataSolicitacaoDesc(Long petId);
    List<ConsultaTriagem> findByStatusOrderByDataSolicitacaoDesc(StatusConsulta status);
    List<ConsultaTriagem> findAllByOrderByDataSolicitacaoDesc();
}
