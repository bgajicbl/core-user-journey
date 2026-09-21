package com.myedspace.cuj.repository;

import com.myedspace.cuj.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
