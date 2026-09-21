package com.myedspace.cuj.security;

import com.myedspace.cuj.domain.Student;
import com.myedspace.cuj.exception.ApiException;
import com.myedspace.cuj.repository.StudentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the calling {@link Student} from the "Authorization: Bearer <jwt>" header.
 * Called explicitly by controllers guarding LMS endpoints.
 */
@Component
@RequiredArgsConstructor
public class CurrentStudentResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final StudentRepository studentRepository;

    public Student resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw ApiException.unauthorized("Missing or malformed Authorization header");
        }
        String token = header.substring(BEARER_PREFIX.length());
        Long studentId = jwtService.validateAndGetStudentId(token)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired token"));
        return studentRepository.findByIdWithCourse(studentId)
                .orElseThrow(() -> ApiException.unauthorized("Student no longer exists"));
    }
}
