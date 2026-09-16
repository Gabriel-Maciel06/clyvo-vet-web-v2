package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.Comissao;
import com.fiap.clyvovet.model.StatusRepasseComissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComissaoRepository extends JpaRepository<Comissao, Long> {
    Optional<Comissao> findByTransacaoId(Long transacaoId);
    List<Comissao> findByClinicaIdOrderByDataPrevisaoRepasseDesc(Long clinicaId);
    List<Comissao> findByStatusRepasse(StatusRepasseComissao status);
}
