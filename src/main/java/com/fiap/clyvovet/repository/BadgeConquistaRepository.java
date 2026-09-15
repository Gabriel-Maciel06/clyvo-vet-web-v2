package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.BadgeConquista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadgeConquistaRepository extends JpaRepository<BadgeConquista, Long> {
    List<BadgeConquista> findByPetIdOrderByDataConquistaDesc(Long petId);
    boolean existsByPetIdAndCodigoBadge(Long petId, String codigoBadge);

    @Modifying
    @Query("DELETE FROM BadgeConquista b WHERE b.pet.id = :petId")
    void deleteByPetId(@Param("petId") Long petId);
}
