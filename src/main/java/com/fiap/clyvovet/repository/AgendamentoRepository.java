package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.Agendamento;
import com.fiap.clyvovet.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByTutorCpfOrderByDataHoraAgendamentoDesc(String tutorCpf);
    List<Agendamento> findByPetIdOrderByDataHoraAgendamentoDesc(Long petId);
    List<Agendamento> findByClinicaIdAndStatusAgendamento(Long clinicaId, StatusAgendamento status);
}
