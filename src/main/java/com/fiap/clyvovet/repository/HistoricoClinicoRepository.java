package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.HistoricoClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoClinicoRepository extends JpaRepository<HistoricoClinico, Long> {
    List<HistoricoClinico> findByPetIdOrderByDataRegistroDesc(Long petId);

    @Modifying
    @Query("DELETE FROM HistoricoClinico h WHERE h.pet.id = :petId")
    void deleteByPetId(@Param("petId") Long petId);
}
