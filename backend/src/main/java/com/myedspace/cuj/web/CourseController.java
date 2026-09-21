package com.myedspace.cuj.web;

import com.myedspace.cuj.exception.ApiException;
import com.myedspace.cuj.repository.CourseRepository;
import com.myedspace.cuj.web.dto.CourseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepository;

    @GetMapping
    public List<CourseDto> listCourses() {
        return courseRepository.findAll().stream()
                .map(CourseDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CourseDto getCourse(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(CourseDto::from)
                .orElseThrow(() -> ApiException.notFound("Course not found: " + id));
    }
}
