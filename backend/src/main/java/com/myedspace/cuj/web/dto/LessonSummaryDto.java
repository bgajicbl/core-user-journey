package com.myedspace.cuj.web.dto;

import com.myedspace.cuj.domain.Lesson;

public record LessonSummaryDto(Long id, String title, int orderIndex) {

    public static LessonSummaryDto from(Lesson lesson) {
        return new LessonSummaryDto(lesson.getId(), lesson.getTitle(), lesson.getOrderIndex());
    }
}
