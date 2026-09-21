package com.myedspace.cuj.web.dto;

import java.util.List;

public record DashboardResponse(String studentName, CourseDto course, List<LessonSummaryDto> lessons) {
}
