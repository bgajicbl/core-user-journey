package com.myedspace.cuj.web;

import com.myedspace.cuj.domain.Course;
import com.myedspace.cuj.domain.Lesson;
import com.myedspace.cuj.domain.Student;
import com.myedspace.cuj.exception.ApiException;
import com.myedspace.cuj.repository.LessonRepository;
import com.myedspace.cuj.security.CurrentStudentResolver;
import com.myedspace.cuj.web.dto.CourseDto;
import com.myedspace.cuj.web.dto.DashboardResponse;
import com.myedspace.cuj.web.dto.LessonDetailDto;
import com.myedspace.cuj.web.dto.LessonSummaryDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Authenticated-only LMS access: dashboard + lesson content, scoped to the student's purchased course. */
@RestController
@RequestMapping("/api/lms")
@RequiredArgsConstructor
public class LmsController {

    private final LessonRepository lessonRepository;
    private final CurrentStudentResolver currentStudentResolver;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(HttpServletRequest request) {
        Student student = currentStudentResolver.resolve(request);
        Course course = student.getPurchase().getCourse();

        List<LessonSummaryDto> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()).stream()
                .map(LessonSummaryDto::from)
                .toList();

        return new DashboardResponse(student.getName(), CourseDto.from(course), lessons);
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonDetailDto lesson(HttpServletRequest request, @PathVariable Long lessonId) {
        Student student = currentStudentResolver.resolve(request);
        Course course = student.getPurchase().getCourse();

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> ApiException.notFound("Lesson not found"));

        if (!lesson.getCourse().getId().equals(course.getId())) {
            throw ApiException.forbidden("This lesson is not part of your course");
        }

        return LessonDetailDto.from(lesson);
    }
}
