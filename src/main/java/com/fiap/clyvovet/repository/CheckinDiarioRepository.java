package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.CheckinDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinDiarioRepository extends JpaRepository<CheckinDiario, Long> {
    List<CheckinDiario> findByPetIdOrderByDataCheckinDesc(Long petId);
    Optional<CheckinDiario> findByPetIdAndDataCheckin(Long petId, LocalDate dataCheckin);
    long countByPetId(Long petId);
    List<CheckinDiario> findByAlertaGeradoTrueOrderByDataCheckinDesc();
}
