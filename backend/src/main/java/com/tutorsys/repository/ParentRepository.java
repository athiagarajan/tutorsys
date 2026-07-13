package com.tutorsys.repository;

import com.tutorsys.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByUserUsername(String username);
    Optional<Parent> findByEmail(String email);
}
