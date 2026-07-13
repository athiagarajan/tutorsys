package com.tutorsys.repository;

import com.tutorsys.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByDeletedFalse();
    List<Student> findByParentIdAndDeletedFalse(Long parentId);
    List<Student> findByParentUserUsernameAndDeletedFalse(String username);
}
