package com.myedspace.cuj.web;

import com.myedspace.cuj.domain.Course;
import com.myedspace.cuj.domain.Purchase;
import com.myedspace.cuj.repository.CourseRepository;
import com.myedspace.cuj.repository.PurchaseRepository;
import com.myedspace.cuj.repository.StudentRepository;
import com.myedspace.cuj.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private PurchaseRepository purchaseRepository;

    // WebConfig registers AuthInterceptor globally, so @WebMvcTest always constructs it —
    // these two are its dependencies, not CheckoutController's, but still need mocking here.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private StudentRepository studentRepository;

    @Test
    void checkoutForAnUnknownCourseReturns404() throws Exception {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":999,\"parentEmail\":\"parent@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkoutWithMissingParentEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutForAKnownCourseCreatesAPendingPurchase() throws Exception {
        Course course = Course.builder()
                .id(1L).subject("Maths").yearRange("Year 5 - 13").price(new BigDecimal("199.00")).build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase purchase = invocation.getArgument(0);
            purchase.setId(1L);
            return purchase;
        });

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":1,\"parentEmail\":\"parent@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.subject").value("Maths"))
                .andExpect(jsonPath("$.invitationToken").isNotEmpty());
    }
}
