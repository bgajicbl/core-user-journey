package com.myedspace.cuj.web;

import com.myedspace.cuj.domain.Course;
import com.myedspace.cuj.domain.Purchase;
import com.myedspace.cuj.domain.PurchaseStatus;
import com.myedspace.cuj.exception.ApiException;
import com.myedspace.cuj.repository.CourseRepository;
import com.myedspace.cuj.repository.PurchaseRepository;
import com.myedspace.cuj.web.dto.CheckoutRequest;
import com.myedspace.cuj.web.dto.CheckoutResponse;
import com.myedspace.cuj.web.dto.CourseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Mock checkout: no payment gateway is involved, per the assignment brief.
 * A successful "purchase" immediately produces an invitation token that
 * stands in for the access path sent to the parent/student.
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CourseRepository courseRepository;
    private final PurchaseRepository purchaseRepository;

    @PostMapping
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found: " + request.courseId()));

        Purchase purchase = purchaseRepository.save(Purchase.builder()
                .course(course)
                .parentEmail(request.parentEmail())
                .invitationToken(UUID.randomUUID().toString())
                .status(PurchaseStatus.PENDING_ONBOARDING)
                .createdAt(Instant.now())
                .build());

        return new CheckoutResponse(purchase.getId(), purchase.getInvitationToken(), CourseDto.from(course));
    }
}
