package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByTutorCpfOrderByIdAsc(String tutorCpf);
}
