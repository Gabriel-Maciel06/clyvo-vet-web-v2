package com.fiap.clyvovet.repository;

import com.fiap.clyvovet.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, String> {
    Optional<Tutor> findByUsuarioUsername(String username);
}
