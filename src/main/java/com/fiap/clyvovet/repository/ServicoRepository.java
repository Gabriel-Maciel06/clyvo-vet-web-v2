package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findByClinicaIdAndAtivoTrue(Long clinicaId);
    List<Servico> findByAtivoTrue();

    /** Resolve a linha de catalogo correspondente a um valor de TipoServicoPreventivo. */
    Optional<Servico> findFirstByCodigoServicoAppAndAtivoTrue(String codigoServicoApp);
}
