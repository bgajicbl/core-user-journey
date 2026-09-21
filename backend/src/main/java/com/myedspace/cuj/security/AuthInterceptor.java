package com.myedspace.cuj.security;

import com.myedspace.cuj.domain.Student;
import com.myedspace.cuj.exception.ApiException;
import com.myedspace.cuj.repository.StudentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces authentication on every path it's registered against in WebConfig, regardless of
 * whether the handler method remembers to check anything itself — a new endpoint added under a
 * protected path pattern is authenticated automatically, rather than relying on every controller
 * method to opt in.
 *
 * Resolves the caller from the "Authorization: Bearer <jwt>" header and stashes it as a request
 * attribute for {@link CurrentStudentArgumentResolver} to inject into {@code @CurrentStudent}
 * controller parameters.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    static final String STUDENT_ATTRIBUTE = AuthInterceptor.class.getName() + ".student";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final StudentRepository studentRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw ApiException.unauthorized("Missing or malformed Authorization header");
        }

        String token = header.substring(BEARER_PREFIX.length());
        Long studentId = jwtService.validateAndGetStudentId(token)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired token"));
        Student student = studentRepository.findByIdWithCourse(studentId)
                .orElseThrow(() -> ApiException.unauthorized("Student no longer exists"));

        request.setAttribute(STUDENT_ATTRIBUTE, student);
        return true;
    }
}
