package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.AgendamentoServico;
import com.fiap.clyvovet.model.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoServicoRepository extends JpaRepository<AgendamentoServico, Long> {

    List<AgendamentoServico> findByTutorCpfOrderByDataCriacaoDesc(String tutorCpf);

    List<AgendamentoServico> findByPetIdOrderByDataCriacaoDesc(Long petId);

    Optional<AgendamentoServico> findByCodigoVoucher(String codigoVoucher);

    List<AgendamentoServico> findByStatusPagamentoOrderByDataCriacaoDesc(StatusPagamento statusPagamento);
}
