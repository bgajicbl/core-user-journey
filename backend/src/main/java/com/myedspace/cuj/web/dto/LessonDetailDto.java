package com.myedspace.cuj.web.dto;

import com.myedspace.cuj.domain.Lesson;

public record LessonDetailDto(Long id, String title, String content, int orderIndex) {

    public static LessonDetailDto from(Lesson lesson) {
        return new LessonDetailDto(lesson.getId(), lesson.getTitle(), lesson.getContent(), lesson.getOrderIndex());
    }
}
