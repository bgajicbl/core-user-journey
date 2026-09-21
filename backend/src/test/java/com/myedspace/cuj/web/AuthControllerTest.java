package com.myedspace.cuj.web;

import com.myedspace.cuj.domain.Student;
import com.myedspace.cuj.repository.StudentRepository;
import com.myedspace.cuj.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test for AuthController, covering the login timing-safety fix: a request for
 * an unknown email must still run a BCrypt comparison (against a dummy hash) rather than
 * short-circuiting, so an attacker can't distinguish "no such account" from "wrong password" by
 * response latency. Asserting the comparison actually happened proves that; a plain status-code
 * check ("both return 401") would not, since the vulnerable version also returned 401 — it just
 * did so faster for an unknown email.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void unknownEmailStillRunsAPasswordComparisonAndReturns401() throws Exception {
        when(studentRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever123\"}"))
                .andExpect(status().isUnauthorized());

        verify(passwordEncoder).matches(eq("whatever123"), anyString());
    }

    @Test
    void wrongPasswordForAKnownEmailReturns401() throws Exception {
        Student student = Student.builder()
                .id(1L).email("student@example.com").passwordHash("stored-hash").name("Ada").createdAt(Instant.now()).build();
        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctCredentialsReturnAToken() throws Exception {
        Student student = Student.builder()
                .id(1L).email("student@example.com").passwordHash("stored-hash").name("Ada").createdAt(Instant.now()).build();
        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("correct", "stored-hash")).thenReturn(true);
        when(jwtService.issueToken(1L, "student@example.com")).thenReturn("dummy.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"correct\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy.jwt.token"))
                .andExpect(jsonPath("$.studentName").value("Ada"));
    }
}
