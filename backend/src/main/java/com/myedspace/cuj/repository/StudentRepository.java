package com.myedspace.cuj.repository;

import com.myedspace.cuj.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmailIgnoreCase(String email);

    @Query("select s from Student s join fetch s.purchase p join fetch p.course where s.id = :id")
    Optional<Student> findByIdWithCourse(Long id);
}
