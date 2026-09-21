package com.myedspace.cuj.web;

import com.myedspace.cuj.domain.Student;
import com.myedspace.cuj.exception.ApiException;
import com.myedspace.cuj.repository.StudentRepository;
import com.myedspace.cuj.security.JwtService;
import com.myedspace.cuj.web.dto.AuthResponse;
import com.myedspace.cuj.web.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * A real BCrypt hash we compare against when no account matches the given email, so a
     * request for an unknown email costs the same BCrypt comparison as one for a known email
     * with the wrong password — otherwise the two cases are distinguishable by response time.
     */
    private static final String DUMMY_PASSWORD_HASH =
            new BCryptPasswordEncoder().encode("dummy-password-used-only-to-equalize-login-timing");

    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Optional<Student> student = studentRepository.findByEmailIgnoreCase(request.email());
        String hashToCheck = student.map(Student::getPasswordHash).orElse(DUMMY_PASSWORD_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (student.isEmpty() || !passwordMatches) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        String jwt = jwtService.issueToken(student.get().getId(), student.get().getEmail());
        return new AuthResponse(jwt, student.get().getId(), student.get().getName());
    }
}
