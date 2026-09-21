package com.myedspace.cuj.web;

import com.myedspace.cuj.domain.Course;
import com.myedspace.cuj.repository.CourseRepository;
import com.myedspace.cuj.repository.StudentRepository;
import com.myedspace.cuj.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseRepository courseRepository;

    // WebConfig registers AuthInterceptor globally, so @WebMvcTest always constructs it —
    // these two are its dependencies, not CourseController's, but still need mocking here.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private StudentRepository studentRepository;

    @Test
    void listsSeededCourses() throws Exception {
        when(courseRepository.findAll()).thenReturn(List.of(
                Course.builder().id(1L).subject("Maths").yearRange("Year 5 - 13").price(new BigDecimal("199.00")).build()));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Maths"))
                .andExpect(jsonPath("$[0].price").value(199.00));
    }
}
