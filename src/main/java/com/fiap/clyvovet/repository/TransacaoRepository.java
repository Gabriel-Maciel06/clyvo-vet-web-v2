package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.StatusTransacao;
import com.fiap.clyvovet.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    Optional<Transacao> findByCodigoVoucher(String codigoVoucher);
    Optional<Transacao> findByCodigoTransacaoGateway(String codigoTransacaoGateway);
    Optional<Transacao> findByAgendamentoId(Long agendamentoId);
    List<Transacao> findByStatusTransacao(StatusTransacao status);
}
