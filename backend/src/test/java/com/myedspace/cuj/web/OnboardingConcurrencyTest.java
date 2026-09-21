package com.myedspace.cuj.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myedspace.cuj.domain.Purchase;
import com.myedspace.cuj.domain.PurchaseStatus;
import com.myedspace.cuj.repository.PurchaseRepository;
import com.myedspace.cuj.repository.StudentRepository;
import com.myedspace.cuj.web.dto.CheckoutRequest;
import com.myedspace.cuj.web.dto.CheckoutResponse;
import com.myedspace.cuj.web.dto.CourseDto;
import com.myedspace.cuj.web.dto.OnboardingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Regression tests for the onboarding race condition fixed in {@link OnboardingController}.
 *
 * Before the fix, a request that lost a race (same invitation token used twice, or the same
 * email used across two different tokens, both submitted concurrently) surfaced as a raw,
 * unhandled 500 once it hit the database's unique-constraint rejection — {@code @Transactional}
 * plus the {@code DataIntegrityViolationException} mapping in {@code GlobalExceptionHandler}
 * turns that into a clean 409, and guarantees exactly one {@code Student} is ever committed
 * per {@code Purchase}.
 *
 * Requests are fired from separate threads via {@link MockMvc}, synchronized with a
 * {@link CyclicBarrier} so they hit the real (H2-backed) transactional service concurrently
 * rather than the race being merely simulated.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OnboardingConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void concurrentOnboardingForTheSameInvitationToken_onlyOneSucceeds() throws Exception {
        String token = purchaseCourse();

        List<MvcResult> results = fireConcurrently(4, i ->
                submitOnboarding(token, "Racer " + i, "same-token-racer-" + i + "-" + token + "@example.com"));

        assertExactlyOneSuccessAndRestAreCleanConflicts(results);

        long purchaseId = purchaseRepository.findByInvitationToken(token).orElseThrow().getId();
        assertThat(studentRepository.findAll().stream()
                .filter(s -> s.getPurchase().getId().equals(purchaseId))
                .count())
                .as("exactly one Student should ever be committed for a given Purchase")
                .isEqualTo(1);

        Purchase purchase = purchaseRepository.findById(purchaseId).orElseThrow();
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.COMPLETED);
    }

    @Test
    void concurrentOnboardingWithTheSameEmailAcrossDifferentTokens_onlyOneSucceeds() throws Exception {
        String sharedEmail = "same-email-racer-" + UUID.randomUUID() + "@example.com";
        List<String> tokens = IntStream.range(0, 4).mapToObj(i -> purchaseCourse()).toList();

        List<MvcResult> results = fireConcurrently(tokens.size(), i ->
                submitOnboarding(tokens.get(i), "Racer " + i, sharedEmail));

        assertExactlyOneSuccessAndRestAreCleanConflicts(results);

        assertThat(studentRepository.findByEmailIgnoreCase(sharedEmail))
                .as("exactly one Student should ever be committed for a given email")
                .isPresent();
    }

    @Test
    void reusingAnAlreadyCompletedInvitationSequentially_returnsConflictNotServerError() throws Exception {
        String token = purchaseCourse();

        MvcResult first = submitOnboarding(token, "First Student", "first-" + token + "@example.com");
        assertThat(first.getResponse().getStatus()).isEqualTo(200);

        MvcResult second = submitOnboarding(token, "Second Student", "second-" + token + "@example.com");
        assertThat(second.getResponse().getStatus()).isEqualTo(409);
    }

    /** Purchases a random seeded course for a unique parent email and returns the invitation token. */
    private String purchaseCourse() {
        try {
            String coursesJson = mockMvc.perform(get("/api/courses"))
                    .andReturn().getResponse().getContentAsString();
            CourseDto[] courses = objectMapper.readValue(coursesJson, CourseDto[].class);

            String checkoutJson = mockMvc.perform(post("/api/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CheckoutRequest(courses[0].id(), "parent-" + UUID.randomUUID() + "@example.com"))))
                    .andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(checkoutJson, CheckoutResponse.class).invitationToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MvcResult submitOnboarding(String token, String studentName, String email) {
        try {
            return mockMvc.perform(post("/api/onboarding/" + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OnboardingRequest(studentName, email, "password123"))))
                    .andReturn();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs {@code count} tasks on separate threads, synchronized with a {@link CyclicBarrier}
     * so they all fire at (as close to) the same instant as the JVM's scheduler allows.
     */
    private List<MvcResult> fireConcurrently(int count, IntFunction<MvcResult> task) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(count);
        ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            List<Callable<MvcResult>> callables = IntStream.range(0, count)
                    .<Callable<MvcResult>>mapToObj(i -> () -> {
                        barrier.await();
                        return task.apply(i);
                    })
                    .toList();

            List<Future<MvcResult>> futures = executor.invokeAll(callables, 30, TimeUnit.SECONDS);
            return futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }
    }

    private void assertExactlyOneSuccessAndRestAreCleanConflicts(List<MvcResult> results) {
        assertThat(results)
                .as("no request should ever surface as an unhandled server error")
                .noneMatch(r -> r.getResponse().getStatus() >= 500);

        long successCount = results.stream().filter(r -> r.getResponse().getStatus() == 200).count();
        long conflictCount = results.stream().filter(r -> r.getResponse().getStatus() == 409).count();

        assertThat(successCount).as("exactly one racer should win").isEqualTo(1);
        assertThat(conflictCount).as("every loser should get a clean 409").isEqualTo(results.size() - 1);
    }
}
