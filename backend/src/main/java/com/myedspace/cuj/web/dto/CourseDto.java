package com.myedspace.cuj.web.dto;

import com.myedspace.cuj.domain.Course;

import java.math.BigDecimal;

public record CourseDto(Long id, String subject, String yearRange, BigDecimal price) {

    public static CourseDto from(Course course) {
        return new CourseDto(course.getId(), course.getSubject(), course.getYearRange(), course.getPrice());
    }
}
