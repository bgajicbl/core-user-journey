package com.myedspace.cuj.web.dto;

public record CheckoutResponse(Long purchaseId, String invitationToken, CourseDto course) {
}
