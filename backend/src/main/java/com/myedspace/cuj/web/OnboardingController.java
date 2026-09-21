package com.myedspace.cuj.web;

import com.myedspace.cuj.domain.Purchase;
import com.myedspace.cuj.domain.PurchaseStatus;
import com.myedspace.cuj.domain.Student;
import com.myedspace.cuj.exception.ApiException;
import com.myedspace.cuj.repository.PurchaseRepository;
import com.myedspace.cuj.repository.StudentRepository;
import com.myedspace.cuj.security.JwtService;
import com.myedspace.cuj.web.dto.AuthResponse;
import com.myedspace.cuj.web.dto.CourseDto;
import com.myedspace.cuj.web.dto.OnboardingInfoResponse;
import com.myedspace.cuj.web.dto.OnboardingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final PurchaseRepository purchaseRepository;
    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @GetMapping("/{token}")
    public OnboardingInfoResponse getOnboardingInfo(@PathVariable String token) {
        Purchase purchase = findPurchaseByToken(token);
        return new OnboardingInfoResponse(
                purchase.getParentEmail(),
                purchase.getStatus().name(),
                CourseDto.from(purchase.getCourse()));
    }

    @PostMapping("/{token}")
    public AuthResponse completeOnboarding(@PathVariable String token, @Valid @RequestBody OnboardingRequest request) {
        Purchase purchase = findPurchaseByToken(token);
        if (purchase.getStatus() != PurchaseStatus.PENDING_ONBOARDING) {
            throw ApiException.conflict("This invitation has already been used");
        }
        if (studentRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Student student = studentRepository.save(Student.builder()
                .purchase(purchase)
                .name(request.studentName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .createdAt(Instant.now())
                .build());

        purchase.setStatus(PurchaseStatus.COMPLETED);
        purchaseRepository.save(purchase);

        String jwt = jwtService.issueToken(student.getId(), student.getEmail());
        return new AuthResponse(jwt, student.getId(), student.getName());
    }

    private Purchase findPurchaseByToken(String token) {
        return purchaseRepository.findByInvitationToken(token)
                .orElseThrow(() -> ApiException.notFound("Invitation not found"));
    }
}
