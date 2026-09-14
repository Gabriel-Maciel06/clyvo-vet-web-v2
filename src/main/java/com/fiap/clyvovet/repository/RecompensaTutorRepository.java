package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.RecompensaTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecompensaTutorRepository extends JpaRepository<RecompensaTutor, Long> {
    Optional<RecompensaTutor> findByTutorCpf(String tutorCpf);
}
