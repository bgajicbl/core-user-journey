package com.myedspace.cuj.config;

import com.myedspace.cuj.domain.Course;
import com.myedspace.cuj.domain.Lesson;
import com.myedspace.cuj.repository.CourseRepository;
import com.myedspace.cuj.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Seeds the sample MyEdSpace courses/lessons from the assignment brief on startup. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    private static final Map<String, List<String>> LESSON_TITLES = Map.of(
            "Maths", List.of("Introduction to Algebra", "Fractions and Decimals", "Geometry Basics"),
            "English", List.of("Grammar Fundamentals", "Creative Writing", "Reading Comprehension"),
            "Science", List.of("Introduction to Biology", "States of Matter", "Forces and Motion")
    );

    @Override
    public void run(String... args) {
        if (courseRepository.count() > 0) {
            return;
        }

        seedCourse("Maths", "Year 5 - 13", new BigDecimal("199.00"));
        seedCourse("English", "Year 5 - 13", new BigDecimal("199.00"));
        seedCourse("Science", "Year 5 - 11", new BigDecimal("199.00"));
    }

    private void seedCourse(String subject, String yearRange, BigDecimal price) {
        Course course = courseRepository.save(Course.builder()
                .subject(subject)
                .yearRange(yearRange)
                .price(price)
                .build());

        List<String> titles = LESSON_TITLES.get(subject);
        for (int i = 0; i < titles.size(); i++) {
            lessonRepository.save(Lesson.builder()
                    .course(course)
                    .title(titles.get(i))
                    .content("This is placeholder content for \"" + titles.get(i) + "\". "
                            + "In a real MES course this would contain the lesson material.")
                    .orderIndex(i)
                    .build());
        }
    }
}
